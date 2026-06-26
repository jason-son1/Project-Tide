package com.tide.rpg.tideext;

import com.tide.core.tide.TideState;
import com.tide.core.tide.TideStateProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bioluminescent Gathering (1-2): only during SPRING_TIDE at night, sand
 * blocks at the water's edge near players have a small chance to sprout a
 * glowing spore (GLOW_LICHEN) that can be harvested with the tide_extractor
 * tool. Scoped to a short radius around online players, not a world scan.
 */
public final class BioluminescentManager {

    private static final double SPAWN_CHANCE_PER_PLAYER = 0.15;
    private static final int SEARCH_RADIUS = 6;

    private final JavaPlugin plugin;
    private final TideStateProvider tideStateProvider;
    private final Set<Location> activeSpores = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    public BioluminescentManager(JavaPlugin plugin, TideStateProvider tideStateProvider) {
        this.plugin = plugin;
        this.tideStateProvider = tideStateProvider;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        clearAllSpores();
    }

    public boolean isActiveSpore(Location location) {
        return activeSpores.contains(normalize(location));
    }

    public void removeSpore(Location location) {
        activeSpores.remove(normalize(location));
    }

    private void tick() {
        if (tideStateProvider.getCurrentState() != TideState.SPRING_TIDE) {
            if (!activeSpores.isEmpty()) {
                clearAllSpores();
            }
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isNight(player) || activeSpores.size() > 200) {
                continue;
            }
            if (ThreadLocalRandom.current().nextDouble() >= SPAWN_CHANCE_PER_PLAYER) {
                continue;
            }
            Block shoreBlock = findShoreBlock(player);
            if (shoreBlock == null) {
                continue;
            }
            Block above = shoreBlock.getRelative(0, 1, 0);
            if (above.getType() != Material.AIR) {
                continue;
            }
            above.setType(Material.GLOW_LICHEN);
            activeSpores.add(normalize(above.getLocation()));
            above.getWorld().spawnParticle(Particle.GLOW, above.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3);
        }
    }

    private Block findShoreBlock(Player player) {
        Location base = player.getLocation();
        for (int attempt = 0; attempt < 5; attempt++) {
            int dx = ThreadLocalRandom.current().nextInt(-SEARCH_RADIUS, SEARCH_RADIUS + 1);
            int dz = ThreadLocalRandom.current().nextInt(-SEARCH_RADIUS, SEARCH_RADIUS + 1);
            Block candidate = base.getWorld().getHighestBlockAt(base.clone().add(dx, 0, dz));
            if (candidate.getType() == Material.SAND) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isNight(Player player) {
        long time = player.getWorld().getTime();
        return time >= 13000 && time <= 23000;
    }

    private void clearAllSpores() {
        for (Location location : activeSpores) {
            Block block = location.getBlock();
            if (block.getType() == Material.GLOW_LICHEN) {
                block.setType(Material.AIR);
            }
        }
        activeSpores.clear();
    }

    private Location normalize(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
