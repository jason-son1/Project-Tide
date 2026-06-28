package com.tide.core.economy;

import java.util.UUID;

/**
 * Cross-jar contract for clam/pearl/reputation. TideRPG and TideMobs
 * obtain this via Bukkit's ServicesManager rather than a direct class reference.
 */
public interface EconomyAPI {

    long getClam(UUID uuid);

    void addClam(UUID uuid, long amount);

    boolean takeClam(UUID uuid, long amount);

    long getPearl(UUID uuid);

    void addPearl(UUID uuid, long amount);

    boolean takePearl(UUID uuid, long amount);

    void addRep(UUID uuid, int amount);

    int getRep(UUID uuid);

    RepTier getRepTier(UUID uuid);

    boolean isHardMode(UUID uuid);

    void setHardMode(UUID uuid, boolean hardMode);

    /** Highest GearScore this player has ever reached — never decreases, so
     *  temporarily unequipping gear can't be used to dodge difficulty scaling. */
    int getPeakGearScore(UUID uuid);

    /** Raises the stored peak GearScore if {@code gearScore} exceeds it; a no-op otherwise. */
    void updatePeakGearScoreIfHigher(UUID uuid, int gearScore);

    /** Progression Index: w1*peakGearScore + w2*rep, weights from difficulty-scaling.pi-weights. */
    double getProgressionIndex(UUID uuid);
}
