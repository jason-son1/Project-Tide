package com.tide.rpg.zone;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

/** Cuboid region with a recommended/warning Gear Score, per zone_config.yml. */
public final class ZoneDefinition {

    private final String id;
    private final String label;
    private final String world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final int recommendedGs;
    private final int warnGs;
    private final int recommendedPartySize;

    private ZoneDefinition(String id, String label, String world, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ, int recommendedGs, int warnGs, int recommendedPartySize) {
        this.id = id;
        this.label = label;
        this.world = world;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.recommendedGs = recommendedGs;
        this.warnGs = warnGs;
        this.recommendedPartySize = recommendedPartySize;
    }

    public static ZoneDefinition parse(YamlConfiguration yaml) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("필수 필드 누락: id");
        }
        return new ZoneDefinition(
                id,
                yaml.getString("label", id),
                yaml.getString("world", "world"),
                yaml.getInt("bounds.min.x"), yaml.getInt("bounds.min.y"), yaml.getInt("bounds.min.z"),
                yaml.getInt("bounds.max.x"), yaml.getInt("bounds.max.y"), yaml.getInt("bounds.max.z"),
                yaml.getInt("recommended_gs", 100),
                yaml.getInt("warn_gs", 50),
                yaml.getInt("recommended_party_size", 1)
        );
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(world)) {
            return false;
        }
        return location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockY() >= minY && location.getBlockY() <= maxY
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getRecommendedGs() {
        return recommendedGs;
    }

    public int getWarnGs() {
        return warnGs;
    }

    public int getRecommendedPartySize() {
        return recommendedPartySize;
    }
}
