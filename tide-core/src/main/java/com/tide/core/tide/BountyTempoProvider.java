package com.tide.core.tide;

import org.bukkit.entity.Player;

/**
 * Interface to allow cross-module tempo modifications of bounty cycles
 * from tide-core to tide-mobs without compile-time dependencies.
 */
public interface BountyTempoProvider {
    long getDailyResetIntervalMinutes();
    void setDailyResetIntervalMinutes(long minutes);
    long getWeeklyResetIntervalMinutes();
    void setWeeklyResetIntervalMinutes(long minutes);
    void forceReset(Player player);
}
