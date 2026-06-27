package com.tide.mobs.boss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Carves an enclosed boss arena chamber around a {@link SoulAltar}'s
 * activation block, similar in spirit to how DeepMineManager procedurally
 * carves its dungeon rooms. Previously {@link AltarBuilder} only placed a
 * 5x5 decorative platform directly onto whatever terrain happened to be at
 * the registered coordinates — so an altar registered via YAML (rather than
 * the in-game /altar create, which already called AltarBuilder) showed up
 * as bare terrain with nothing built. This builds the actual room first;
 * AltarBuilder then decorates its center exactly as before.
 */
public final class BossArenaBuilder {

    private static final int RADIUS = 11;
    private static final int WALL_THICKNESS = 2;
    private static final int FLOOR_Y = -1;
    private static final int CEILING_Y = 8;

    private BossArenaBuilder() {
    }

    public static void build(Location center, String bossType) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        Theme theme = themeFor(bossType);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int outerRadius = RADIUS + WALL_THICKNESS;

        for (int dx = -outerRadius; dx <= outerRadius; dx++) {
            for (int dz = -outerRadius; dz <= outerRadius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > outerRadius) {
                    continue;
                }
                if (dist <= RADIUS) {
                    forceBlock(world, cx + dx, cy + FLOOR_Y, cz + dz, theme.floor());
                    for (int y = FLOOR_Y + 1; y < CEILING_Y; y++) {
                        forceBlock(world, cx + dx, cy + y, cz + dz, Material.AIR);
                    }
                    forceBlock(world, cx + dx, cy + CEILING_Y, cz + dz, theme.ceiling());
                } else {
                    for (int y = FLOOR_Y; y <= CEILING_Y; y++) {
                        forceBlock(world, cx + dx, cy + y, cz + dz, theme.wall());
                    }
                }
            }
        }

        carveEntrance(world, cx, cy, cz, theme);
        placeWallLanterns(world, cx, cy, cz, theme);
    }

    /** A walk-in tunnel punched south through the wall shell so the arena isn't sealed. */
    private static void carveEntrance(World world, int cx, int cy, int cz, Theme theme) {
        for (int dz = RADIUS - 1; dz <= RADIUS + WALL_THICKNESS + 3; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                forceBlock(world, cx + dx, cy + FLOOR_Y, cz + dz, theme.floor());
                for (int y = 0; y <= 3; y++) {
                    forceBlock(world, cx + dx, cy + y, cz + dz, Material.AIR);
                }
            }
        }
    }

    private static void placeWallLanterns(World world, int cx, int cy, int cz, Theme theme) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] dir : directions) {
            int lx = cx + dir[0] * (RADIUS - 1);
            int lz = cz + dir[1] * (RADIUS - 1);
            forceBlock(world, lx, cy + 3, lz, theme.accent());
        }
    }

    private static void forceBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(material, false);
    }

    private static Theme themeFor(String bossType) {
        return switch (bossType == null ? "" : bossType.toUpperCase()) {
            case "CORAL_QUEEN" -> new Theme(Material.DARK_PRISMARINE, Material.PRISMARINE_BRICKS, Material.SEA_LANTERN, Material.PRISMARINE);
            case "ABYSSAL_TITAN" -> new Theme(Material.POLISHED_BLACKSTONE, Material.DEEPSLATE_TILES, Material.DEEPSLATE_BRICKS, Material.SOUL_LANTERN);
            default -> new Theme(Material.POLISHED_BLACKSTONE, Material.BLACKSTONE, Material.CRYING_OBSIDIAN, Material.SEA_LANTERN); // VOID_KNIGHT
        };
    }

    private record Theme(Material floor, Material wall, Material ceiling, Material accent) {
    }
}
