package com.tide.rpg.tideext;

import com.tide.core.tide.TideState;
import com.tide.core.tide.TideStateProvider;
import com.tide.rpg.TideKeys;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Tidal Currents (1-1): during the last {@code transitionWindowSeconds} before
 * a HIGH_TIDE<->LOW_TIDE flip, players standing in water get pushed — toward
 * shore as the tide comes in, toward open water as it goes out. Force follows
 * sin(pi * progress) so it peaks mid-transition and fades at both ends.
 * Special states (SPRING_TIDE/BLOOD_MOON/BLOOD_TIDE) don't drive a current —
 * only the base HIGH/LOW alternation does.
 */
public final class TidalCurrentManager {

    private final JavaPlugin plugin;
    private final TideStateProvider tideStateProvider;
    private final long transitionWindowSeconds;
    private final double baseStrength;
    private final Vector shoreDirection;
    private BukkitTask task;

    public TidalCurrentManager(JavaPlugin plugin, TideStateProvider tideStateProvider,
                                long transitionWindowSeconds, double baseStrength, Vector shoreDirection) {
        this.plugin = plugin;
        this.tideStateProvider = tideStateProvider;
        this.transitionWindowSeconds = Math.max(1, transitionWindowSeconds);
        this.baseStrength = baseStrength;
        this.shoreDirection = shoreDirection.clone().normalize();
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void tick() {
        TideState state = tideStateProvider.getCurrentState();
        if (state != TideState.HIGH_TIDE && state != TideState.LOW_TIDE) {
            return;
        }

        long secondsRemaining = tideStateProvider.getSecondsUntilNextChange();
        if (secondsRemaining > transitionWindowSeconds) {
            return;
        }

        double progress = 1.0 - (secondsRemaining / (double) transitionWindowSeconds);
        double forceScale = Math.sin(progress * Math.PI) * baseStrength;
        if (forceScale <= 0.001) {
            return;
        }

        // LOW_TIDE 종료 임박 = 물이 들어오는 중 = 해안 방향. HIGH_TIDE 종료 임박 = 물이 빠지는 중 = 먼바다 방향.
        Vector direction = state == TideState.LOW_TIDE ? shoreDirection : shoreDirection.clone().multiply(-1);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isInWater(player)) {
                continue;
            }
            double resistance = anchorResistance(player);
            if (resistance >= 1.0) {
                continue;
            }
            Vector push = direction.clone().multiply(forceScale * (1.0 - resistance));
            player.setVelocity(player.getVelocity().add(push));
        }
    }

    private boolean isInWater(Player player) {
        return player.isSwimming() || player.getLocation().getBlock().isLiquid();
    }

    private double anchorResistance(Player player) {
        var boots = player.getInventory().getBoots();
        if (boots == null || boots.getItemMeta() == null) {
            return 0;
        }
        Integer anchor = boots.getItemMeta().getPersistentDataContainer().get(TideKeys.ANCHOR, PersistentDataType.INTEGER);
        if (anchor == null) {
            return 0;
        }
        return Math.min(1.0, anchor * 0.3);
    }
}
