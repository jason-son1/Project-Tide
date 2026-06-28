package com.tide.mobs.affix;

import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;

public final class AffixDefinition {

    private final String id;
    private final String displayName;
    private final double hpMultiplier;
    private final double damageMultiplier;
    private final Particle spawnParticle;
    private final double weight;

    private AffixDefinition(String id, String displayName, double hpMultiplier,
                             double damageMultiplier, Particle spawnParticle, double weight) {
        this.id = id;
        this.displayName = displayName;
        this.hpMultiplier = hpMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.spawnParticle = spawnParticle;
        this.weight = weight;
    }

    public static AffixDefinition parse(YamlConfiguration yaml) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("필수 필드 누락: id");
        }
        Particle particle = null;
        String particleName = yaml.getString("spawn_particle");
        if (particleName != null) {
            try {
                particle = Particle.valueOf(particleName.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new AffixDefinition(
                id,
                yaml.getString("display_name", id),
                yaml.getDouble("hp_multiplier", 1.0),
                yaml.getDouble("damage_multiplier", 1.0),
                particle,
                yaml.getDouble("weight", 10.0)
        );
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getHpMultiplier() {
        return hpMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public Particle getSpawnParticle() {
        return spawnParticle;
    }

    public double getWeight() {
        return weight;
    }
}
