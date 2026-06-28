package com.tide.mobs.difficulty;

import com.tide.core.difficulty.DifficultyManager;
import com.tide.core.difficulty.DifficultyResult;
import com.tide.mobs.MobKeys;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Spawn-time difficulty scaling (EliteSpawnListener/CustomMobSpawnListener) only
 * captures the world's difficulty at the instant a mob is born — a mob that
 * spawned five minutes ago keeps fighting at five-minutes-ago strength even as
 * players gear up or the tide state shifts. This task periodically re-resolves
 * and re-applies WDM to every already-loaded monster so the world catches up
 * without waiting for a respawn cycle. Boss-altar mobs are excluded; they're
 * managed by BossFightManager's own scaling.
 */
public final class WorldDifficultyRefreshTask {

    private final JavaPlugin plugin;
    private final DifficultyManager difficultyManager;
    private BukkitTask task;

    public WorldDifficultyRefreshTask(JavaPlugin plugin, DifficultyManager difficultyManager) {
        this.plugin = plugin;
        this.difficultyManager = difficultyManager;
    }

    public void start() {
        if (task != null) {
            return;
        }
        long intervalTicks = plugin.getConfig().getLong("difficulty-scaling.refresh-interval-ticks", 100);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void refreshAll() {
        if (difficultyManager == null || !difficultyManager.isEnabled()) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getEntitiesByClass(Monster.class)) {
                if (entity.getPersistentDataContainer().has(MobKeys.BOSS_MARKER, PersistentDataType.STRING)) {
                    continue;
                }
                DifficultyResult difficulty = difficultyManager.resolve(entity.getLocation());
                WorldDifficultyApplier.apply(entity, difficulty);
            }
        }
    }
}
