package com.tide.core.glow;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * entity.setGlowing(true) renders a team-color outline regardless of distance
 * or line-of-sight, so 레어/정예/네메시스 몹 glow was visible across nearly the
 * whole render distance. This registers glow-worthy mobs and toggles glow
 * off/on based on whether any player is within range, re-checked on a
 * low-frequency timer over only the bounded set of registered mobs (not a
 * per-tick world scan).
 */
public final class GlowRangeManager {

    private final Map<UUID, Double> rangeByEntity = new HashMap<>();
    private BukkitTask task;

    public void start(JavaPlugin plugin) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        rangeByEntity.clear();
    }

    /** Registers an entity for distance-gated glow. Use instead of calling entity.setGlowing(true) directly. */
    public void register(LivingEntity entity, double range) {
        rangeByEntity.put(entity.getUniqueId(), range);
        entity.setGlowing(false);
    }

    public void unregister(LivingEntity entity) {
        rangeByEntity.remove(entity.getUniqueId());
    }

    private void tick() {
        Iterator<Map.Entry<UUID, Double>> iterator = rangeByEntity.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Double> entry = iterator.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) {
                iterator.remove();
                continue;
            }
            double range = entry.getValue();
            boolean nearby = !living.getWorld().getNearbyPlayers(living.getLocation(), range).isEmpty();
            living.setGlowing(nearby);
        }
    }
}
