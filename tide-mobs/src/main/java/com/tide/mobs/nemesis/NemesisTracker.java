package com.tide.mobs.nemesis;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 20-tick periodic check over active nemesis records only (typically zero or
 * one per online player) — not a scan of every entity in the world. Once a
 * record has evolved into a Calamity (3-1), it stops being a 1:1 stalker and
 * starts roaming for *any* nearby player — a server-wide raid threat.
 */
public final class NemesisTracker {

    private static final double TRACK_RADIUS = 150;
    private static final double RAID_RADIUS = 300;
    private static final double RAID_RETARGET_CHANCE = 0.05; // ~once every 20s on average at a 20-tick period

    private final JavaPlugin plugin;
    private final NemesisManager nemesisManager;
    private final CalamityManager calamityManager;
    private BukkitTask task;

    public NemesisTracker(JavaPlugin plugin, NemesisManager nemesisManager, CalamityManager calamityManager) {
        this.plugin = plugin;
        this.nemesisManager = nemesisManager;
        this.calamityManager = calamityManager;
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
            Entity mobEntity = Bukkit.getEntity(record.getMobUuid());
            if (!(mobEntity instanceof Mob mob) || mob.isDead() || !mob.isValid()) {
                continue;
            }

            if (calamityManager.hasEvolved(record.getMobUuid())
                    && ThreadLocalRandom.current().nextDouble() < RAID_RETARGET_CHANCE) {
                Player raidTarget = findNearestPlayer(mob, RAID_RADIUS);
                if (raidTarget != null) {
                    mob.setTarget(raidTarget);
                    continue;
                }
            }

            Player target = Bukkit.getPlayer(record.getPlayerUuid());
            if (target == null || !target.isOnline()) {
                continue; // paused while the target is offline
            }
            if (!mob.getWorld().equals(target.getWorld())) {
                continue;
            }
            if (mob.getLocation().distance(target.getLocation()) <= TRACK_RADIUS) {
                mob.setTarget(target);
            }
        }
    }

    private Player findNearestPlayer(Mob mob, double radius) {
        Player nearest = null;
        double bestDistance = radius;
        for (Player player : mob.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(mob.getLocation());
            if (distance <= bestDistance) {
                bestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }
}
