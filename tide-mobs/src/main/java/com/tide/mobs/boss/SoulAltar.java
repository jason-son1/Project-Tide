package com.tide.mobs.boss;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SoulAltar {

    private final String id;
    private final String world;
    private final int x, y, z;
    private final int requiredFragments;
    private final int recommendedPartySize;

    private SoulAltar(String id, String world, int x, int y, int z, int requiredFragments, int recommendedPartySize) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.requiredFragments = requiredFragments;
        this.recommendedPartySize = recommendedPartySize;
    }

    public static SoulAltar parse(YamlConfiguration yaml) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("필수 필드 누락: id");
        }
        return new SoulAltar(
                id,
                yaml.getString("world", "world"),
                yaml.getInt("block.x"),
                yaml.getInt("block.y"),
                yaml.getInt("block.z"),
                yaml.getInt("required_fragments", 5),
                yaml.getInt("recommended_party_size", 3)
        );
    }

    public boolean matchesBlock(Location location) {
        return location.getWorld() != null && location.getWorld().getName().equals(world)
                && location.getBlockX() == x && location.getBlockY() == y && location.getBlockZ() == z;
    }

    public Location summonLocation() {
        return new Location(org.bukkit.Bukkit.getWorld(world), x + 0.5, y + 1, z + 0.5);
    }

    public String getId() {
        return id;
    }

    public int getRequiredFragments() {
        return requiredFragments;
    }

    public int getRecommendedPartySize() {
        return recommendedPartySize;
    }
}
