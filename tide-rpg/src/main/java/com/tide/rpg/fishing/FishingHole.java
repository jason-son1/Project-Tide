package com.tide.rpg.fishing;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

public final class FishingHole {

    private final String id;
    private final String world;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    private FishingHole(String id, String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.id = id;
        this.world = world;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public static FishingHole parse(YamlConfiguration yaml) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("필수 필드 누락: id");
        }
        return new FishingHole(id, yaml.getString("world", "world"),
                yaml.getInt("bounds.min.x"), yaml.getInt("bounds.min.y"), yaml.getInt("bounds.min.z"),
                yaml.getInt("bounds.max.x"), yaml.getInt("bounds.max.y"), yaml.getInt("bounds.max.z"));
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
}
