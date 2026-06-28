package com.tide.mobs.boss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Carves an enclosed boss arena chamber underground.
 * Improvements over the original:
 *  - Pre-clears the full sphere + wall shell with AIR before building to prevent
 *    cave systems bleeding into the arena.
 *  - Applies random cracked/mossy variants to the outer wall for an aged look.
 *  - Adds scattered soul lanterns and chains inside as ambient decoration.
 */
public final class BossArenaBuilder {

    private static final int RADIUS = 11;
    private static final int WALL_THICKNESS = 2;
    private static final int FLOOR_Y = -1;
    private static final int CEILING_Y = 8;
    private static final int ENTRANCE_TUNNEL_LENGTH = RADIUS + WALL_THICKNESS + 3;

    private BossArenaBuilder() {}

    /** How far south of the arena center the entrance tunnel's outer mouth sits. */
    public static int entranceTunnelLength() {
        return ENTRANCE_TUNNEL_LENGTH;
    }

    /** The walkable point at the entrance tunnel's outer (south) mouth, floor-level+1 —
     *  i.e. where a surface shaft should land so it connects straight into the tunnel
     *  instead of dead-ending against the arena's outer wall. */
    public static Location entranceMouth(Location center) {
        return center.clone().add(0, FLOOR_Y + 1, ENTRANCE_TUNNEL_LENGTH);
    }

    public static void build(Location center, String bossType) {
        World world = center.getWorld();
        if (world == null) return;

        Theme theme = themeFor(bossType);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int outerRadius = RADIUS + WALL_THICKNESS;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // ── PASS 1: Pre-clear entire sphere bounding box with AIR ────────────────
        // This prevents cave systems from visually intersecting the arena.
        for (int dx = -outerRadius; dx <= outerRadius; dx++) {
            for (int dy = FLOOR_Y - 1; dy <= CEILING_Y + 1; dy++) {
                for (int dz = -outerRadius; dz <= outerRadius; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist <= outerRadius) {
                        Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                        if (b.getType() != Material.BEDROCK)
                            b.setType(Material.AIR, false);
                    }
                }
            }
        }

        // ── PASS 2: Build the arena shell ─────────────────────────────────────────
        for (int dx = -outerRadius; dx <= outerRadius; dx++) {
            for (int dz = -outerRadius; dz <= outerRadius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > outerRadius) continue;

                if (dist <= RADIUS) {
                    // Interior
                    forceBlock(world, cx + dx, cy + FLOOR_Y, cz + dz, theme.floor());
                    for (int dy = FLOOR_Y + 1; dy < CEILING_Y; dy++)
                        forceBlock(world, cx + dx, cy + dy, cz + dz, Material.AIR);
                    forceBlock(world, cx + dx, cy + CEILING_Y, cz + dz, theme.ceiling());
                } else {
                    // Wall ring — apply aged/worn variants randomly
                    for (int dy = FLOOR_Y; dy <= CEILING_Y; dy++) {
                        Material wallMat = weatheredVariant(theme.wall(), rng);
                        forceBlock(world, cx + dx, cy + dy, cz + dz, wallMat);
                    }
                }
            }
        }

        // ── PASS 3: Entrance tunnel (south) ───────────────────────────────────────
        carveEntrance(world, cx, cy, cz, theme);

        // ── PASS 4: Wall lanterns + chain decoration ──────────────────────────────
        placeWallLanterns(world, cx, cy, cz, theme, rng);

        // ── PASS 5: Floor scatter (bones, cracked tiles) ─────────────────────────
        scatterFloorDecor(world, cx, cy, cz, rng, theme);

        // ── PASS 6: CORAL_QUEEN gets a flooded chamber — Elder Guardians barely move and
        //    can't path on dry land, so fighting one in an empty stone room (the old behavior)
        //    just looked broken. The entrance tunnel itself stays dry/walkable.
        if ("CORAL_QUEEN".equalsIgnoreCase(bossType)) {
            floodInterior(world, cx, cy, cz);
        }
    }

    private static void floodInterior(World world, int cx, int cy, int cz) {
        for (int dx = -(RADIUS - 1); dx <= RADIUS - 1; dx++) {
            for (int dz = -(RADIUS - 1); dz <= RADIUS - 1; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) > RADIUS - 1) continue;
                for (int dy = FLOOR_Y + 1; dy <= FLOOR_Y + 4; dy++) {
                    Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    if (b.getType() == Material.AIR) {
                        b.setType(Material.WATER, false);
                    }
                }
            }
        }
    }

    /** Punches a 3-wide entrance tunnel out through the south wall. */
    private static void carveEntrance(World world, int cx, int cy, int cz, Theme theme) {
        for (int dz = RADIUS - 1; dz <= RADIUS + WALL_THICKNESS + 3; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                forceBlock(world, cx + dx, cy + FLOOR_Y, cz + dz, theme.floor());
                for (int dy = 0; dy <= 3; dy++)
                    forceBlock(world, cx + dx, cy + dy, cz + dz, Material.AIR);
                // Doorframe
                forceBlock(world, cx + dx, cy + 4, cz + dz, theme.wall());
            }
            // Side walls of tunnel
            for (int dy = 0; dy <= 4; dy++) {
                forceBlock(world, cx - 2, cy + dy, cz + dz, theme.wall());
                forceBlock(world, cx + 2, cy + dy, cz + dz, theme.wall());
            }
        }
        // Arch over entrance
        for (int dx = -1; dx <= 1; dx++) {
            forceBlock(world, cx + dx, cy + 4, cz + RADIUS - 1, theme.wall());
        }
    }

    private static void placeWallLanterns(World world, int cx, int cy, int cz, Theme theme, ThreadLocalRandom rng) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] dir : directions) {
            int lx = cx + dir[0] * (RADIUS - 1);
            int lz = cz + dir[1] * (RADIUS - 1);
            forceBlock(world, lx, cy + 3, lz, theme.accent());
            // Chain above lantern
            if (rng.nextDouble() < 0.7) {
                forceBlock(world, lx, cy + 4, lz, Material.CHAIN);
                forceBlock(world, lx, cy + 5, lz, Material.CHAIN);
            }
        }
    }

    private static void scatterFloorDecor(World world, int cx, int cy, int cz, ThreadLocalRandom rng, Theme theme) {
        for (int dx = -(RADIUS - 2); dx <= RADIUS - 2; dx++) {
            for (int dz = -(RADIUS - 2); dz <= RADIUS - 2; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > RADIUS - 2) continue;
                // Skip center
                if (Math.abs(dx) < 3 && Math.abs(dz) < 3) continue;

                double roll = rng.nextDouble();
                if (roll < 0.03) {
                    forceBlock(world, cx + dx, cy + FLOOR_Y, cz + dz, Material.BONE_BLOCK);
                } else if (roll < 0.05) {
                    forceBlock(world, cx + dx, cy + FLOOR_Y, cz + dz, Material.CRACKED_STONE_BRICKS);
                } else if (roll < 0.055) {
                    forceBlock(world, cx + dx, cy + FLOOR_Y + 1, cz + dz, Material.COBWEB);
                }
            }
        }
    }

    /** Returns a weathered variant of the wall material ~30% of the time. */
    private static Material weatheredVariant(Material base, ThreadLocalRandom rng) {
        if (rng.nextDouble() > 0.30) return base;
        return switch (base) {
            case DEEPSLATE_BRICKS       -> rng.nextBoolean() ? Material.CRACKED_DEEPSLATE_BRICKS  : Material.MOSSY_COBBLESTONE;
            case STONE_BRICKS           -> rng.nextBoolean() ? Material.CRACKED_STONE_BRICKS       : Material.MOSSY_STONE_BRICKS;
            case POLISHED_BLACKSTONE    -> Material.CRACKED_POLISHED_BLACKSTONE_BRICKS;
            default -> base;
        };
    }

    private static void forceBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != Material.BEDROCK)
            block.setType(material, false);
    }

    private static Theme themeFor(String bossType) {
        return switch (bossType == null ? "" : bossType.toUpperCase()) {
            case "CORAL_QUEEN"   -> new Theme(Material.DARK_PRISMARINE, Material.PRISMARINE_BRICKS,  Material.SEA_LANTERN,       Material.PRISMARINE);
            case "ABYSSAL_TITAN" -> new Theme(Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES, Material.DEEPSLATE_BRICKS,  Material.SOUL_LANTERN);
            default              -> new Theme(Material.POLISHED_BLACKSTONE, Material.BLACKSTONE,      Material.CRYING_OBSIDIAN,   Material.SEA_LANTERN);
        };
    }

    private record Theme(Material floor, Material wall, Material ceiling, Material accent) {}
}
