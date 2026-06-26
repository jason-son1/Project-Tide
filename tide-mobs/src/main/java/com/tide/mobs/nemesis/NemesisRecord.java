package com.tide.mobs.nemesis;

import java.util.UUID;

public final class NemesisRecord {

    private UUID mobUuid;
    private final UUID playerUuid;
    private final String originalName;
    private final String affixesCsv;
    private int killCount;
    private boolean active;

    public NemesisRecord(UUID mobUuid, UUID playerUuid, String originalName, String affixesCsv,
                          int killCount, boolean active) {
        this.mobUuid = mobUuid;
        this.playerUuid = playerUuid;
        this.originalName = originalName;
        this.affixesCsv = affixesCsv;
        this.killCount = killCount;
        this.active = active;
    }

    public UUID getMobUuid() {
        return mobUuid;
    }

    public void setMobUuid(UUID mobUuid) {
        this.mobUuid = mobUuid;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getAffixesCsv() {
        return affixesCsv;
    }

    public int getKillCount() {
        return killCount;
    }

    public void incrementKillCount() {
        killCount++;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
