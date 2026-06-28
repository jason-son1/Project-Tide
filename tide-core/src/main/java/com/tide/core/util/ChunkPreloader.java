package com.tide.core.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Pre-loads every chunk a procedural structure's bounding box will touch, asynchronously,
 * before any block is written. Writing blocks near the edge of a hand-rolled structure
 * (boss arenas, dungeon portals) easily reaches past the chunk that triggered the generation
 * roll — without this, {@code World#getBlockAt} on an unloaded neighbor forces a synchronous
 * load/generate right there in the chunk-load event handler, which is exactly the kind of
 * surprise recursive load that causes lag spikes. Pre-loading the whole box async first makes
 * the cost explicit, bounded, and off the main thread until the structure is actually ready
 * to build.
 */
public final class ChunkPreloader {

    private ChunkPreloader() {
    }

    /**
     * @param centerX/centerZ block coordinates at the center of the structure's footprint
     * @param radius          how far the structure's block writes can reach from the center, in blocks
     * @param onReady         runs on the main thread once every required chunk is loaded
     */
    public static void preload(Plugin plugin, World world, int centerX, int centerZ, int radius, Runnable onReady) {
        int minChunkX = (centerX - radius) >> 4;
        int maxChunkX = (centerX + radius) >> 4;
        int minChunkZ = (centerZ - radius) >> 4;
        int maxChunkZ = (centerZ + radius) >> 4;

        List<CompletableFuture<org.bukkit.Chunk>> futures = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                futures.add(world.getChunkAtAsync(cx, cz, true));
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, onReady));
    }
}
