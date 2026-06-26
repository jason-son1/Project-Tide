package com.tide.core.death;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "조류에 휩쓸림" — items never truly drop on death; they're held by an
 * ArmorStand grave the owner can reclaim within the time limit.
 */
public final class GraveManager {

    private final JavaPlugin plugin;
    private final Map<UUID, WreckageGrave> gravesByOwner = new ConcurrentHashMap<>();
    private final Map<UUID, WreckageGrave> gravesByStandId = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public GraveManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
        }
    }

    public WreckageGrave create(Player owner, Location location, List<ItemStack> items, long durationSeconds) {
        WreckageGrave existing = gravesByOwner.get(owner.getUniqueId());
        if (existing != null) {
            existing.getArmorStand().remove();
            gravesByStandId.remove(existing.getArmorStand().getUniqueId());
        }

        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setCustomNameVisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setGravity(false);
        armorStand.setMarker(false);
        armorStand.setBasePlate(true);

        WreckageGrave grave = new WreckageGrave(armorStand.getUniqueId(), owner.getUniqueId(), armorStand, items, durationSeconds);
        gravesByOwner.put(owner.getUniqueId(), grave);
        gravesByStandId.put(armorStand.getUniqueId(), grave);
        updateName(grave);
        com.tide.core.TideCorePlugin.getInstance().getEffectEngine().playEffect(location, "grave_create");
        return grave;
    }

    public boolean hasActiveGrave(UUID ownerUuid) {
        return gravesByOwner.containsKey(ownerUuid);
    }

    public WreckageGrave getGrave(UUID ownerUuid) {
        return gravesByOwner.get(ownerUuid);
    }

    public WreckageGrave getGraveByStand(UUID standUuid) {
        return gravesByStandId.get(standUuid);
    }

    public boolean recover(Player player, WreckageGrave grave) {
        if (!grave.getOwnerUuid().equals(player.getUniqueId())) {
            return false;
        }
        for (ItemStack item : grave.getItems()) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        removeGrave(grave);
        com.tide.core.TideCorePlugin.getInstance().getEffectEngine().playEffect(player, "grave_reclaim");
        return true;
    }

    private void tick() {
        for (WreckageGrave grave : List.copyOf(gravesByOwner.values())) {
            grave.decrementSecond();
            if (grave.getRemainingSeconds() <= 0) {
                removeGrave(grave);
            } else {
                updateName(grave);
            }
        }
    }

    private void updateName(WreckageGrave grave) {
        long minutes = grave.getRemainingSeconds() / 60;
        long seconds = grave.getRemainingSeconds() % 60;
        grave.getArmorStand().setCustomName(String.format("§b[유실물 비석] §7남은 시간: %02d:%02d", minutes, seconds));
    }

    private void removeGrave(WreckageGrave grave) {
        grave.getArmorStand().remove();
        gravesByOwner.remove(grave.getOwnerUuid());
        gravesByStandId.remove(grave.getArmorStand().getUniqueId());
    }

    public List<String> buildHudLines(Player player) {
        WreckageGrave grave = gravesByOwner.get(player.getUniqueId());
        if (grave == null) {
            return List.of();
        }
        Location graveLocation = grave.getArmorStand().getLocation();
        double distance = player.getWorld().equals(graveLocation.getWorld())
                ? player.getLocation().distance(graveLocation) : -1;
        long minutes = grave.getRemainingSeconds() / 60;
        long seconds = grave.getRemainingSeconds() % 60;
        return List.of(
                "§c[유실물 비석]",
                "§7거리: §f" + (distance >= 0 ? Math.round(distance) + "m" : "다른 차원"),
                "§7남은 시간: §e" + String.format("%d:%02d", minutes, seconds),
                "§7좌표: §f" + graveLocation.getBlockX() + ", " + graveLocation.getBlockY() + ", " + graveLocation.getBlockZ()
        );
    }
}
