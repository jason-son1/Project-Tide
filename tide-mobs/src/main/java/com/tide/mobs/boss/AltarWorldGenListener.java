package com.tide.mobs.boss;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public final class AltarWorldGenListener implements Listener {

    private final JavaPlugin plugin;
    private final AltarRegistry altarRegistry;

    public AltarWorldGenListener(JavaPlugin plugin, AltarRegistry altarRegistry) {
        this.plugin = plugin;
        this.altarRegistry = altarRegistry;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) return;

        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        String targetWorld = plugin.getConfig().getString("altar.worldgen-world", "world");
        if (!world.getName().equalsIgnoreCase(targetWorld)) return;

        double chance = plugin.getConfig().getDouble("altar.worldgen-chance", 0.001);
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;

        int blockX = chunk.getX() * 16 + ThreadLocalRandom.current().nextInt(4, 12);
        int blockZ = chunk.getZ() * 16 + ThreadLocalRandom.current().nextInt(4, 12);

        // Find actual solid ground block, bypassing water, leaves, or air
        int groundY = -999;
        for (int y = 120; y >= -40; y--) {
            Material t = world.getBlockAt(blockX, y, blockZ).getType();
            if (t.isSolid() && t != Material.BARRIER && !t.name().contains("LEAVES")) {
                groundY = y;
                break;
            }
        }
        if (groundY == -999) return;

        final int finalGroundY = groundY;
        Location baseLoc = new Location(world, blockX, finalGroundY, blockZ);
        Material type = baseLoc.getBlock().getType();
        if (!isNaturalGround(type)) return;

        // The shaft lands at the entrance tunnel's outer mouth (the validated surface column);
        // the actual arena chamber is carved further north, underground, by BossArenaBuilder —
        // never directly under raw unmodified terrain, which is what used to leave boss spawn
        // points embedded in solid rock or open caves ("floating in midair") depending on luck.
        int minPlayableY = world.getMinHeight() + 16;
        int mouthY = Math.max(finalGroundY - 22, minPlayableY);
        Location mouthLoc = new Location(world, blockX, mouthY, blockZ);
        Location altarLoc = mouthLoc.clone().add(0, 0, -BossArenaBuilder.entranceTunnelLength());

        // Biome-based boss distribution (computed before the lambda so variables are effectively final)
        org.bukkit.block.Biome biome = world.getBiome(blockX, finalGroundY, blockZ);
        String biomeName = biome.name().toUpperCase();
        String bossType;
        if (biomeName.contains("OCEAN") || biomeName.contains("RIVER") || biomeName.contains("BEACH")) {
            bossType = "CORAL_QUEEN";
        } else if (biomeName.contains("DARK_FOREST") || biomeName.contains("SWAMP") || biomeName.contains("SPOOKY") || biomeName.contains("NETHER")) {
            bossType = "ABYSSAL_TITAN";
        } else {
            // Land biomes: 70% VOID_KNIGHT, 30% ABYSSAL_TITAN
            bossType = ThreadLocalRandom.current().nextDouble() < 0.70 ? "VOID_KNIGHT" : "ABYSSAL_TITAN";
        }
        final String finalBossType = bossType;

        String displayName = switch (finalBossType) {
            case "CORAL_QUEEN"   -> "§3§l산호 여왕";
            case "ABYSSAL_TITAN" -> "§5§l심연의 거신";
            default              -> "§4§l공허의 기사";
        };
        final String finalDisplayName = displayName;

        // The arena sits ~16 blocks south of the surface point and itself spans a further
        // ±13-block radius — well past the single chunk that triggered this roll. Pre-load the
        // whole footprint asynchronously first so writing those blocks never forces a surprise
        // synchronous load of a neighboring chunk from inside this chunk-load handler.
        com.tide.core.util.ChunkPreloader.preload(plugin, world, blockX, blockZ, 30, () -> {
            File altarsDir = new File(plugin.getDataFolder(), "altars");
            if (!altarsDir.exists()) altarsDir.mkdirs();

            int idx = 1;
            String id;
            File outFile;
            do {
                id = "altar_auto_" + idx;
                outFile = new File(altarsDir, id + ".yml");
                idx++;
            } while (outFile.exists());

            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("id", id);
            yaml.set("world", world.getName());
            yaml.set("block.x", altarLoc.getBlockX());
            yaml.set("block.y", altarLoc.getBlockY());
            yaml.set("block.z", altarLoc.getBlockZ());
            yaml.set("required_fragments", 5);
            yaml.set("recommended_party_size", 3);
            yaml.set("boss_type", finalBossType);
            yaml.set("boss_display_name", finalDisplayName);

            try {
                yaml.save(outFile);
                BossArenaBuilder.build(altarLoc, finalBossType);
                AltarBuilder.build(altarLoc);
                buildSurfaceShrine(world, baseLoc, mouthLoc, biomeName);
                altarRegistry.reload();
                plugin.getLogger().info("[보스제단 월드젠] (" + altarLoc.getBlockX() + ", " + altarLoc.getBlockY() + ", " + altarLoc.getBlockZ()
                        + ") 지하에 '" + id + "' (" + finalBossType + ") 생성 완료.");
            } catch (IOException e) {
                plugin.getLogger().warning("보스제단 자동 월드젠 파일 저장 실패: " + e.getMessage());
            }
        });
    }

    private void buildSurfaceShrine(World world, Location surfaceCenter, Location mouthLoc, String biomeName) {
        int cx = surfaceCenter.getBlockX();
        int cy = surfaceCenter.getBlockY();
        int cz = surfaceCenter.getBlockZ();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Material[] floorPalette = biomeFloorPalette(biomeName);
        Material[] edgePalette = biomeEdgePalette(biomeName);

        // 1. Terrain-aware platform: each (x,z) column filled from highestY down to cy
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean outerRing = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                int localHighest = world.getHighestBlockYAt(cx + dx, cz + dz);
                // Fill from surface down to platform level
                for (int y = localHighest; y >= cy - 3; y--) {
                    Block b = world.getBlockAt(cx + dx, y, cz + dz);
                    if (y == cy) {
                        // Noise-style edge feathering: the outer ring has a chance to keep
                        // whatever natural terrain was already there instead of always being
                        // overwritten, so the platform's boundary looks eroded/grown-over
                        // rather than a perfectly cut square.
                        if (outerRing && rng.nextDouble() < 0.35) {
                            continue;
                        }
                        b.setType(outerRing ? pick(edgePalette, rng) : pick(floorPalette, rng), false);
                    } else if (y > cy) {
                        // Remove any terrain above platform (level it)
                        b.setType(Material.AIR, false);
                    } else {
                        // Fill below platform to prevent floating
                        if (b.getType().isAir() || b.getType() == Material.CAVE_AIR || !b.getType().isSolid())
                            b.setType(Material.STONE_BRICKS, false);
                    }
                }

                // Adaptive foundation: the fixed 3-block pad above isn't guaranteed to land on
                // solid ground on a cliff/overhang — keep extending the support pillar straight
                // down until it actually reaches solid ground (capped, so a floating island
                // doesn't recurse all the way to bedrock).
                int supportY = cy - 4;
                int safety = 0;
                while (safety < 20) {
                    Block below = world.getBlockAt(cx + dx, supportY, cz + dz);
                    if (below.getType().isSolid() && below.getType() != Material.CAVE_AIR) {
                        break;
                    }
                    below.setType(Material.STONE_BRICKS, false);
                    supportY--;
                    safety++;
                }
            }
        }

        // 2. Stair-edged border (outer ring)
        for (int dx = -3; dx <= 3; dx++) {
            placeStair(world, cx + dx, cy, cz - 3, BlockFace.NORTH, rng);
            placeStair(world, cx + dx, cy, cz + 3, BlockFace.SOUTH, rng);
        }
        for (int dz = -2; dz <= 2; dz++) {
            placeStair(world, cx - 3, cy, cz + dz, BlockFace.WEST, rng);
            placeStair(world, cx + 3, cy, cz + dz, BlockFace.EAST, rng);
        }

        // 3. Four corner pillars with chiseled cap + soul lantern
        int[][] corners = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
        for (int[] c : corners) {
            for (int h = 1; h <= 4; h++)
                world.getBlockAt(cx + c[0], cy + h, cz + c[1]).setType(Material.STONE_BRICK_WALL, false);
            world.getBlockAt(cx + c[0], cy + 5, cz + c[1]).setType(Material.CHISELED_STONE_BRICKS, false);
            world.getBlockAt(cx + c[0], cy + 6, cz + c[1]).setType(Material.SOUL_LANTERN, false);
        }

        // 4. Shaft from surface (cy) down to altar Y, with stone brick walls and ladder
        // mouthLoc already sits at the entrance tunnel's walkable floor+1 level (see
        // BossArenaBuilder.entranceMouth), so the shaft lands exactly at the tunnel mouth
        // instead of punching into the arena's solid outer wall a level too low/high.
        int shaftBottomY = mouthLoc.getBlockY();
        for (int y = cy; y >= shaftBottomY; y--) {
            // Clear center
            world.getBlockAt(cx, y, cz).setType(Material.AIR, false);

            // 3x3 shaft wall (leave center empty)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Block wall = world.getBlockAt(cx + dx, y, cz + dz);
                    if (!wall.getType().isSolid() || wall.getType() == Material.AIR || wall.getType() == Material.CAVE_AIR)
                        wall.setType(Material.STONE_BRICKS, false);
                }
            }

            // Ladder on north face of center shaft
            Block ladderWall = world.getBlockAt(cx, y, cz - 1);
            ladderWall.setType(Material.STONE_BRICKS, false);
            Block ladderBlock = world.getBlockAt(cx, y, cz);
            ladderBlock.setType(Material.LADDER, false);
            if (ladderBlock.getBlockData() instanceof org.bukkit.block.data.type.Ladder ladderData) {
                ladderData.setFacing(BlockFace.SOUTH);
                ladderBlock.setBlockData(ladderData, false);
            }

            // Lantern every 8 blocks
            if ((cy - y) % 8 == 4) {
                Block lantern = world.getBlockAt(cx + 1, y, cz);
                if (lantern.getType().isAir() || lantern.getType() == Material.CAVE_AIR)
                    lantern.setType(Material.SEA_LANTERN, false);
            }
        }

        // 5. Entry arch at the bottom of the shaft into the arena
        int archY = shaftBottomY;
        world.getBlockAt(cx - 1, archY, cz - 1).setType(Material.CHISELED_STONE_BRICKS, false);
        world.getBlockAt(cx + 1, archY, cz - 1).setType(Material.CHISELED_STONE_BRICKS, false);
        world.getBlockAt(cx - 1, archY, cz + 1).setType(Material.CHISELED_STONE_BRICKS, false);
        world.getBlockAt(cx + 1, archY, cz + 1).setType(Material.CHISELED_STONE_BRICKS, false);
        world.getBlockAt(cx, archY - 1, cz - 1).setType(Material.SOUL_TORCH, false);
        world.getBlockAt(cx, archY - 1, cz + 1).setType(Material.SOUL_TORCH, false);
    }

    private void placeStair(World world, int x, int y, int z, BlockFace facing, ThreadLocalRandom rng) {
        // 40% chance to skip (worn away)
        if (rng.nextDouble() < 0.40) return;
        Block b = world.getBlockAt(x, y, z);
        b.setType(Material.STONE_BRICK_STAIRS, false);
        if (b.getBlockData() instanceof Stairs stairs) {
            stairs.setFacing(facing);
            b.setBlockData(stairs, false);
        }
    }

    /** Biome-adaptive floor palette so the shrine reads as "built from what's around it"
     *  instead of always being grey stone brick regardless of desert/snow/jungle/swamp. */
    private Material[] biomeFloorPalette(String biomeName) {
        if (biomeName.contains("DESERT")) {
            return new Material[]{Material.SMOOTH_SANDSTONE, Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE,
                    Material.SANDSTONE, Material.CHISELED_SANDSTONE};
        }
        if (biomeName.contains("BADLANDS") || biomeName.contains("MESA")) {
            return new Material[]{Material.RED_SANDSTONE, Material.RED_SANDSTONE, Material.TERRACOTTA,
                    Material.RED_TERRACOTTA, Material.CHISELED_RED_SANDSTONE};
        }
        if (biomeName.contains("SNOWY") || biomeName.contains("ICE") || biomeName.contains("FROZEN")) {
            return new Material[]{Material.POLISHED_ANDESITE, Material.STONE_BRICKS, Material.PACKED_ICE,
                    Material.SNOW_BLOCK, Material.COBBLESTONE};
        }
        if (biomeName.contains("JUNGLE") || biomeName.contains("LUSH")) {
            return new Material[]{Material.MOSSY_STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.MOSS_BLOCK,
                    Material.CRACKED_STONE_BRICKS, Material.MOSSY_COBBLESTONE};
        }
        if (biomeName.contains("SWAMP") || biomeName.contains("MANGROVE")) {
            return new Material[]{Material.MUD_BRICKS, Material.PACKED_MUD, Material.MOSSY_STONE_BRICKS,
                    Material.MOSSY_COBBLESTONE, Material.MUD_BRICKS};
        }
        return new Material[]{Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS, Material.STONE_BRICKS,
                Material.COBBLESTONE, Material.MOSSY_COBBLESTONE};
    }

    /** Edge material is the "most weathered/natural-looking" subset of the same biome palette —
     *  it's what blends into the surrounding ground at the platform's perimeter. */
    private Material[] biomeEdgePalette(String biomeName) {
        if (biomeName.contains("DESERT")) {
            return new Material[]{Material.SAND, Material.SANDSTONE, Material.SAND};
        }
        if (biomeName.contains("BADLANDS") || biomeName.contains("MESA")) {
            return new Material[]{Material.RED_SAND, Material.COARSE_DIRT, Material.RED_TERRACOTTA};
        }
        if (biomeName.contains("SNOWY") || biomeName.contains("ICE") || biomeName.contains("FROZEN")) {
            return new Material[]{Material.SNOW_BLOCK, Material.PACKED_ICE, Material.GRAVEL};
        }
        if (biomeName.contains("JUNGLE") || biomeName.contains("LUSH")) {
            return new Material[]{Material.MOSSY_COBBLESTONE, Material.MOSS_BLOCK, Material.MUDDY_MANGROVE_ROOTS};
        }
        if (biomeName.contains("SWAMP") || biomeName.contains("MANGROVE")) {
            return new Material[]{Material.MUD, Material.PACKED_MUD, Material.MOSSY_COBBLESTONE};
        }
        return new Material[]{Material.MOSSY_COBBLESTONE, Material.COBBLESTONE, Material.GRAVEL};
    }

    private Material pick(Material[] palette, ThreadLocalRandom rng) {
        return palette[rng.nextInt(palette.length)];
    }

    private boolean isNaturalGround(Material type) {
        return type == Material.GRASS_BLOCK || type == Material.DIRT || type == Material.STONE
                || type == Material.SAND || type == Material.DEEPSLATE || type == Material.GRAVEL
                || type == Material.PODZOL || type == Material.COARSE_DIRT;
    }
}
