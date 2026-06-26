package com.tide.mobs.mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CustomMob {

    private final String id;
    private final String displayName;
    private final String baseMob;
    private final int customModelData;
    private final double hpMultiplier;
    private final double damageMultiplier;
    private final double movementSpeed;
    private final List<String> affixes;
    private final List<String> spawnWorlds;
    private final List<String> spawnBiomes;
    private final List<String> spawnTideStates;
    private final int spawnWeight;
    private final List<Map<String, Object>> drops;

    public CustomMob(String id, String displayName, String baseMob, int customModelData,
                     double hpMultiplier, double damageMultiplier, double movementSpeed,
                     List<String> affixes, List<String> spawnWorlds, List<String> spawnBiomes,
                     List<String> spawnTideStates, int spawnWeight, List<Map<String, Object>> drops) {
        this.id = id;
        this.displayName = displayName;
        this.baseMob = baseMob;
        this.customModelData = customModelData;
        this.hpMultiplier = hpMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.movementSpeed = movementSpeed;
        this.affixes = affixes != null ? affixes : new ArrayList<>();
        this.spawnWorlds = spawnWorlds != null ? spawnWorlds : new ArrayList<>();
        this.spawnBiomes = spawnBiomes != null ? spawnBiomes : new ArrayList<>();
        this.spawnTideStates = spawnTideStates != null ? spawnTideStates : new ArrayList<>();
        this.spawnWeight = spawnWeight;
        this.drops = drops != null ? drops : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBaseMob() {
        return baseMob;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public double getHpMultiplier() {
        return hpMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public List<String> getAffixes() {
        return affixes;
    }

    public List<String> getSpawnWorlds() {
        return spawnWorlds;
    }

    public List<String> getSpawnBiomes() {
        return spawnBiomes;
    }

    public List<String> getSpawnTideStates() {
        return spawnTideStates;
    }

    public int getSpawnWeight() {
        return spawnWeight;
    }

    public List<Map<String, Object>> getDrops() {
        return drops;
    }
}
