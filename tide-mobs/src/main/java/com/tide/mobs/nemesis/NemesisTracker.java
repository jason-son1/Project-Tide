package com.tide.mobs.nemesis;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * 20-tick periodic check over active nemesis records only (typically zero or
 * one per online player) — not a scan of every entity in the world.
 */
public final class NemesisTracker {

    private static final double TRACK_RADIUS = 150;

    private final JavaPlugin plugin;
    private final NemesisManager nemesisManager;
    private BukkitTask task;

    public NemesisTracker(JavaPlugin plugin, NemesisManager nemesisManager) {
        this.plugin = plugin;
        this.nemesisManager = nemesisManager;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void tick() {
        for (NemesisRecord record : List.copyOf(nemesisManager.getAllActive())) {
            Player target = Bukkit.getPlayer(record.getPlayerUuid());
            if (target == null || !target.isOnline()) {
                continue; // paused while the target is offline
            }
            Entity mobEntity = Bukkit.getEntity(record.getMobUuid());
            if (!(mobEntity instanceof Mob mob) || mob.isDead() || !mob.isValid()) {
                continue;
            }
            if (!mob.getWorld().equals(target.getWorld())) {
                continue;
            }
            if (mob.getLocation().distance(target.getLocation()) <= TRACK_RADIUS) {
                mob.setTarget(target);
            }
        }
    }
}
