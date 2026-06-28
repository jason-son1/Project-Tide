package com.tide.mobs.boss;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BossInstance {

    private final String altarId;
    private final LivingEntity entity;
    private final long spawnTimeMillis;
    private final Set<UUID> participants = new HashSet<>();
    private int phase = 1;
    private boolean enraged;
    private String bossType = "VOID_KNIGHT";
    private Location spawnLocation;
    private double gearScoreFactor = 1.0;
    private int shieldHitsLeft = 0;
    private boolean shieldActive = false;
    private boolean shieldTriggered = false;

    public BossInstance(String altarId, LivingEntity entity) {
        this.altarId = altarId;
        this.entity = entity;
        this.spawnTimeMillis = System.currentTimeMillis();
    }

    public String getAltarId() {
        return altarId;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - spawnTimeMillis) / 1000;
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public boolean isEnraged() {
        return enraged;
    }

    public void setEnraged(boolean enraged) {
        this.enraged = enraged;
    }

    public String getBossType() { return bossType; }
    public void setBossType(String bossType) { this.bossType = bossType; }

    public Location getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }

    public double getGearScoreFactor() { return gearScoreFactor; }
    public void setGearScoreFactor(double gearScoreFactor) { this.gearScoreFactor = gearScoreFactor; }

    public int getShieldHitsLeft() { return shieldHitsLeft; }
    public void setShieldHitsLeft(int shieldHitsLeft) { this.shieldHitsLeft = shieldHitsLeft; }

    public boolean isShieldActive() { return shieldActive; }
    public void setShieldActive(boolean shieldActive) { this.shieldActive = shieldActive; }

    public boolean isShieldTriggered() { return shieldTriggered; }
    public void setShieldTriggered(boolean shieldTriggered) { this.shieldTriggered = shieldTriggered; }
}
