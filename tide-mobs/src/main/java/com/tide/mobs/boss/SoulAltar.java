package com.tide.mobs.boss;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SoulAltar {

    private final String id;
    private final String world;
    private final int x, y, z;
    private final int requiredFragments;
    private final int recommendedPartySize;
    private final String bossType;
    private final String bossDisplayName;

    private SoulAltar(String id, String world, int x, int y, int z,
                      int requiredFragments, int recommendedPartySize,
                      String bossType, String bossDisplayName) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.requiredFragments = requiredFragments;
        this.recommendedPartySize = recommendedPartySize;
        this.bossType = bossType;
        this.bossDisplayName = bossDisplayName;
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
                yaml.getInt("recommended_party_size", 3),
                yaml.getString("boss_type", "VOID_KNIGHT"),
                yaml.getString("boss_display_name", "§4§l공허의 기사")
        );
    }

    public boolean matchesBlock(Location location) {
        return location.getWorld() != null && location.getWorld().getName().equals(world)
                && location.getBlockX() == x && location.getBlockY() == y && location.getBlockZ() == z;
    }

    public Location summonLocation() {
        return new Location(org.bukkit.Bukkit.getWorld(world), x + 0.5, y + 1, z + 0.5);
    }

    public int getBlockX() { return x; }
    public int getBlockY() { return y; }
    public int getBlockZ() { return z; }
    public String getWorld() { return world; }

    public String getId() { return id; }
    public int getRequiredFragments() { return requiredFragments; }
    public int getRecommendedPartySize() { return recommendedPartySize; }
    public String getBossType() { return bossType; }
    public String getBossDisplayName() { return bossDisplayName; }
}
