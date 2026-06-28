package com.tide.rpg.deepmine;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ThreadLocalRandom;

public final class DeepMineWorldGenListener implements Listener {

    private final Plugin plugin;
    private final DeepMineManagerRegistry registry;

    public DeepMineWorldGenListener(Plugin plugin, DeepMineManagerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) return;

        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        String targetWorld = plugin.getConfig().getString("deepmine.world", "world");
        if (!world.getName().equalsIgnoreCase(targetWorld)) return;

        double chance = plugin.getConfig().getDouble("deepmine.worldgen-chance", 0.002);
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;

        int blockX = chunk.getX() * 16 + ThreadLocalRandom.current().nextInt(4, 12);
        int blockZ = chunk.getZ() * 16 + ThreadLocalRandom.current().nextInt(4, 12);
        Location targetLoc = null;

        for (int y = -10; y >= -50; y--) {
            Material base = world.getBlockAt(blockX, y, blockZ).getType();
            if (!isSolidBase(base)) continue;
            boolean hasSpace = true;
            for (int dy = 1; dy <= 6; dy++) {
                Material above = world.getBlockAt(blockX, y + dy, blockZ).getType();
                if (!above.isAir() && above != Material.CAVE_AIR) { hasSpace = false; break; }
            }
            if (hasSpace) { targetLoc = new Location(world, blockX, y + 1, blockZ); break; }
        }

        if (targetLoc == null) return;

        final Location spawnLoc = targetLoc;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                DeepMineManager manager = registry.createAutoInstance(null);
                if (manager == null) return;
                buildPortalRuin(world, spawnLoc);
                plugin.getLogger().info("[딥마인 월드젠] (" + spawnLoc.getBlockX() + ", " + spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ() + ") 에 심해 광산 차원문 유적 생성 완료.");
            } catch (Exception e) {
                plugin.getLogger().warning("딥마인 자동 월드젠 포탈 배치 실패: " + e.getMessage());
            }
        });
    }

    private void buildPortalRuin(World world, Location origin) {
        int cx = origin.getBlockX();
        int cy = origin.getBlockY();
        int cz = origin.getBlockZ();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = -1; dy <= 5; dy++) {
                    Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    Material t = b.getType();
                    if (t.isSolid() && !isOre(t)) b.setType(Material.AIR, false);
                }
            }
        }

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if ((Math.abs(dx) == 3 || Math.abs(dz) == 3) && rng.nextDouble() < 0.35) continue;
                world.getBlockAt(cx + dx, cy - 1, cz + dz).setType(getFloorMat(), false);
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -4; dy <= -2; dy++) {
                    Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    if (b.getType().isAir() || b.getType() == Material.CAVE_AIR)
                        b.setType(Material.COBBLED_DEEPSLATE, false);
                }
            }
        }

        origin.getBlock().setType(Material.CRYING_OBSIDIAN, false);

        boolean northSouth = rng.nextBoolean();
        buildArch(world, cx, cy, cz, northSouth, rng);
        decorateCavityWalls(world, cx, cy, cz, rng);

        if (northSouth) {
            placeLantern(world, cx, cy + 4, cz - 1);
            placeLantern(world, cx, cy + 4, cz + 1);
        } else {
            placeLantern(world, cx - 1, cy + 4, cz);
            placeLantern(world, cx + 1, cy + 4, cz);
        }
        int[][] torchPos = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}, {-3, 0}, {3, 0}, {0, -3}, {0, 3}};
        for (int[] off : torchPos) {
            if (rng.nextDouble() < 0.55) {
                Block b = world.getBlockAt(cx + off[0], cy, cz + off[1]);
                if (b.getType().isAir() || b.getType() == Material.CAVE_AIR)
                    b.setType(Material.SOUL_TORCH, false);
            }
        }

        int[][] chestCandidates = {{2, 2}, {-2, 2}, {2, -2}, {-2, -2}};
        for (int[] pos : chestCandidates) {
            Block floor = world.getBlockAt(cx + pos[0], cy - 1, cz + pos[1]);
            Block above = world.getBlockAt(cx + pos[0], cy, cz + pos[1]);
            if (floor.getType().isSolid() && (above.getType().isAir() || above.getType() == Material.CAVE_AIR)) {
                above.setType(Material.CHEST, false);
                if (above.getState() instanceof org.bukkit.block.Chest chest) {
                    org.bukkit.inventory.Inventory inv = chest.getInventory();
                    inv.addItem(new org.bukkit.inventory.ItemStack(Material.COAL, rng.nextInt(3, 8)));
                    inv.addItem(new org.bukkit.inventory.ItemStack(Material.RAW_IRON, rng.nextInt(2, 6)));
                    if (rng.nextDouble() < 0.45) inv.addItem(new org.bukkit.inventory.ItemStack(Material.GOLD_NUGGET, rng.nextInt(4, 12)));
                    if (rng.nextDouble() < 0.20) inv.addItem(new org.bukkit.inventory.ItemStack(Material.RAW_GOLD, rng.nextInt(1, 3)));
                    if (rng.nextDouble() < 0.10) inv.addItem(new org.bukkit.inventory.ItemStack(Material.DIAMOND, 1));
                }
                break;
            }
        }

        Location textLoc = origin.clone().add(0.5, 1.5, 0.5);
        world.spawn(textLoc, ArmorStand.class, as -> {
            as.setGravity(false);
            as.setVisible(false);
            as.setCustomName("§d§l[심해 광산 통로] §7우클릭하여 입장");
            as.setCustomNameVisible(true);
            as.setMarker(true);
        });
    }

    private void buildArch(World world, int cx, int cy, int cz, boolean northSouth, ThreadLocalRandom rng) {
        int[][] pillarOffsets = northSouth ? new int[][]{{0, -1}, {0, 1}} : new int[][]{{-1, 0}, {1, 0}};
        for (int[] off : pillarOffsets) {
            int px = cx + off[0];
            int pz = cz + off[1];
            for (int dy = 0; dy <= 3; dy++) {
                Material mat = (dy < 3 && rng.nextDouble() < 0.15) ? Material.COBBLESTONE : getObsidianMat(rng);
                world.getBlockAt(px, cy + dy, pz).setType(mat, false);
            }
        }

        if (northSouth) {
            for (int dz = -1; dz <= 1; dz++) {
                Material mat = (rng.nextDouble() < 0.2) ? Material.CRACKED_STONE_BRICKS : getObsidianMat(rng);
                world.getBlockAt(cx, cy + 3, cz + dz).setType(mat, false);
            }
        } else {
            for (int dx = -1; dx <= 1; dx++) {
                Material mat = (rng.nextDouble() < 0.2) ? Material.CRACKED_STONE_BRICKS : getObsidianMat(rng);
                world.getBlockAt(cx + dx, cy + 3, cz).setType(mat, false);
            }
        }

        int[][] corners = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        for (int[] c : corners) {
            for (int dy = 0; dy <= 2; dy++) {
                if (rng.nextDouble() > 0.40) {
                    Block b = world.getBlockAt(cx + c[0], cy + dy, cz + c[1]);
                    if (b.getType().isAir() || b.getType() == Material.CAVE_AIR)
                        b.setType(getObsidianMat(rng), false);
                }
            }
        }
    }

    private void decorateCavityWalls(World world, int cx, int cy, int cz, ThreadLocalRandom rng) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = -1; dy <= 5; dy++) {
                    Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    if (!b.getType().isAir() && b.getType() != Material.CAVE_AIR) continue;
                    boolean adj = false;
                    for (BlockFace face : faces) { if (b.getRelative(face).getType().isSolid()) { adj = true; break; } }
                    if (!adj) continue;
                    double roll = rng.nextDouble();
                    if (roll < 0.06) b.setType(Material.GLOW_LICHEN, false);
                    else if (roll < 0.09 && dy == 0) b.setType(Material.COBWEB, false);
                }
            }
        }
    }

    private void placeLantern(World world, int x, int y, int z) {
        Block b = world.getBlockAt(x, y, z);
        if (b.getType().isAir() || b.getType() == Material.CAVE_AIR) b.setType(Material.SOUL_LANTERN, false);
    }

    private boolean isSolidBase(Material type) {
        return type == Material.STONE || type == Material.DEEPSLATE || type == Material.TUFF
                || type == Material.ANDESITE || type == Material.DIORITE || type == Material.GRANITE
                || type == Material.CALCITE || type == Material.SMOOTH_BASALT;
    }

    private boolean isOre(Material type) {
        String name = type.name();
        return name.contains("_ORE") || name.contains("AMETHYST");
    }

    private Material getFloorMat() {
        double r = ThreadLocalRandom.current().nextDouble();
        if (r < 0.22) return Material.MOSSY_STONE_BRICKS;
        if (r < 0.42) return Material.CRACKED_STONE_BRICKS;
        if (r < 0.58) return Material.COBBLESTONE;
        if (r < 0.73) return Material.MOSSY_COBBLESTONE;
        if (r < 0.85) return Material.GRAVEL;
        if (r < 0.93) return Material.STONE_BRICKS;
        return Material.COBBLED_DEEPSLATE;
    }

    private Material getObsidianMat(ThreadLocalRandom rng) {
        return rng.nextDouble() < 0.40 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
    }
}