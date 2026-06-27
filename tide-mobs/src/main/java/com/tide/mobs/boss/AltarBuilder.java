package com.tide.mobs.boss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Builds a soul altar structure around a given center block.
 * Call build(location) to place the decorative altar blocks around the activation block.
 *
 * Structure:
 *   - 3x3 Crying Obsidian floor (center = activation block)
 *   - 4 Basalt pillars at corners (height 5), topped with Sea Lanterns
 *   - Soul Fire or Campfire at center top
 *   - Chain decorations connecting pillars
 */
public final class AltarBuilder {

    private AltarBuilder() {}

    /**
     * Builds the altar decoration centered on the given block location.
     * The center block becomes the activation trigger (Crying Obsidian).
     *
     * @param center the block location that will be the altar's activation point
     */
    public static void build(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // ── 1. 3×3 바닥: Crying Obsidian ──────────────────────────────
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setBlock(world, cx + dx, cy - 1, cz + dz, Material.CRYING_OBSIDIAN);
            }
        }
        // Center activation block (Crying Obsidian on same level)
        setBlock(world, cx, cy, cz, Material.CRYING_OBSIDIAN);

        // ── 2. 4개 코너 현무암 기둥 (높이 5) ─────────────────────────
        int[][] corners = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
        for (int[] c : corners) {
            int px = cx + c[0];
            int pz = cz + c[1];

            // Basalt pillar base
            setBlock(world, px, cy - 1, pz, Material.POLISHED_BLACKSTONE);
            for (int h = 0; h <= 4; h++) {
                setBlock(world, px, cy + h, pz, Material.BASALT);
            }
            // Sea lantern on top
            setBlock(world, px, cy + 5, pz, Material.SEA_LANTERN);
            // Blackstone wall decoration
            setBlock(world, px, cy + 6, pz, Material.BLACKSTONE_WALL);
        }

        // ── 3. 제단 중심: Soul Campfire ────────────────────────────────
        setBlock(world, cx, cy + 1, cz, Material.SOUL_CAMPFIRE);

        // ── 4. 기둥 사이 체인 (y+3 높이) ─────────────────────────────
        // North-South chains
        for (int dz = -1; dz <= 1; dz++) {
            setBlock(world, cx - 2, cy + 3, cz + dz, Material.CHAIN);
            setBlock(world, cx + 2, cy + 3, cz + dz, Material.CHAIN);
        }
        // East-West chains
        for (int dx = -1; dx <= 1; dx++) {
            setBlock(world, cx + dx, cy + 3, cz - 2, Material.CHAIN);
            setBlock(world, cx + dx, cy + 3, cz + 2, Material.CHAIN);
        }

        // ── 5. 뼈 블록 외곽 데코 ──────────────────────────────────────
        int[][] outer = {
            {-2, 0}, {2, 0}, {0, -2}, {0, 2},
            {-2, -1}, {-2, 1}, {2, -1}, {2, 1},
            {-1, -2}, {1, -2}, {-1, 2}, {1, 2}
        };
        for (int[] o : outer) {
            setBlock(world, cx + o[0], cy - 1, cz + o[1], Material.BONE_BLOCK);
        }
    }

    private static void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
            block.setType(material, false);
        }
    }
}
