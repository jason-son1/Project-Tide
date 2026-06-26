package com.tide.rpg.deepmine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A self-contained "instance" carved out of one region instead of a full
 * world copy — periodically re-rolls its ore distribution rather than relying
 * on WorldEdit. Block writes are spread across many ticks so a 30-minute
 * reset never causes a single big lag spike.
 */
public final class DeepMineManager {

    private static final Material[] ORE_POOL = {
            Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE
    };
    private static final double[] ORE_WEIGHT = {0.35, 0.25, 0.15, 0.10, 0.08, 0.05, 0.02};
    private static final int BLOCKS_PER_TICK = 4000;

    private final JavaPlugin plugin;
    private final String worldName;
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    private final Location entrance;
    private final long resetIntervalTicks;

    private final Map<UUID, List<org.bukkit.inventory.ItemStack>> sessionLoot = new ConcurrentHashMap<>();
    private BukkitTask resetTask;
    private BukkitTask scheduleTask;

    public DeepMineManager(JavaPlugin plugin, String worldName, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ, Location entrance, long resetIntervalMinutes) {
        this.plugin = plugin;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.entrance = entrance;
        this.resetIntervalTicks = resetIntervalMinutes * 60L * 20L;
    }

    public void start() {
        scheduleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::reset, 20L, resetIntervalTicks);
    }

    public void stop() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
        }
        if (resetTask != null) {
            resetTask.cancel();
        }
    }

    public boolean isInside(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
            return false;
        }
        return location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockY() >= minY && location.getBlockY() <= maxY
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public void enter(Player player) {
        sessionLoot.putIfAbsent(player.getUniqueId(), new java.util.concurrent.CopyOnWriteArrayList<>());
        player.teleport(entrance);
        player.sendMessage("§b딥 마인에 입장했습니다. §7깊이 들어갈수록 위험하지만 보상도 커집니다.");
    }

    public void leave(Player player) {
        sessionLoot.remove(player.getUniqueId());
        player.teleport(entrance);
        player.sendMessage("§a딥 마인에서 안전하게 귀환했습니다. 획득한 전리품을 모두 유지합니다.");
    }

    public void trackLoot(Player player, org.bukkit.inventory.ItemStack itemStack) {
        sessionLoot.computeIfAbsent(player.getUniqueId(), uuid -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(itemStack.clone());
    }

    /** Called on death inside the mine: forces an exit and forfeits part of this session's loot. */
    public void onDeathInside(Player player) {
        List<org.bukkit.inventory.ItemStack> loot = sessionLoot.remove(player.getUniqueId());
        if (loot != null) {
            for (org.bukkit.inventory.ItemStack tracked : loot) {
                if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                    org.bukkit.inventory.ItemStack toRemove = tracked.clone();
                    player.getInventory().removeItem(toRemove);
                }
            }
        }
        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(entrance));
        player.sendMessage("§c광산에서 사망하여 강제로 추방되었습니다. 일부 전리품을 잃었습니다.");
    }

    private void reset() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        Deque<int[]> queue = new ArrayDeque<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    queue.add(new int[]{x, y, z});
                }
            }
        }
        if (resetTask != null) {
            resetTask.cancel();
        }
        plugin.getLogger().info("딥 마인 리셋 시작: " + queue.size() + "개 블록");
        resetTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int processed = 0;
            while (processed < BLOCKS_PER_TICK && !queue.isEmpty()) {
                int[] pos = queue.poll();
                world.getBlockAt(pos[0], pos[1], pos[2]).setType(rollMaterial());
                processed++;
            }
            if (queue.isEmpty()) {
                resetTask.cancel();
                plugin.getLogger().info("딥 마인 리셋 완료.");
            }
        }, 0L, 1L);
    }

    private Material rollMaterial() {
        double roll = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0;
        for (int i = 0; i < ORE_POOL.length; i++) {
            cumulative += ORE_WEIGHT[i];
            if (roll < cumulative) {
                return ORE_POOL[i];
            }
        }
        return Material.STONE;
    }
}
