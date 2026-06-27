package com.tide.rpg.deepmine;

import com.tide.rpg.item.ItemFactory;
import com.tide.rpg.rune.RuneItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns every {@link DeepMineManager} instance. Previously there was exactly
 * one hardcoded in config.yml, so making a second dungeon meant hand-picking
 * non-overlapping coordinates yourself. This reads a list (`deepmine.instances`)
 * instead of a single dict, and {@link #createAutoInstance()} computes a free
 * non-overlapping region next to the existing ones automatically, persists it
 * back to config.yml, and starts it immediately — no manual coordinate math,
 * no restart.
 */
public final class DeepMineManagerRegistry {

    private static final int INSTANCE_SPACING = 64; // blocks of buffer between instance bounding boxes

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final RuneItemFactory runeItemFactory;
    private final Map<String, DeepMineManager> instances = new LinkedHashMap<>();

    public DeepMineManagerRegistry(JavaPlugin plugin, ItemFactory itemFactory, RuneItemFactory runeItemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.runeItemFactory = runeItemFactory;
    }

    public void start() {
        List<Map<?, ?>> rawInstances = plugin.getConfig().getMapList("deepmine.instances");
        if (rawInstances.isEmpty()) {
            // Legacy single-instance schema (deepmine.world/bounds/entrance directly) — keep working as "mine_1".
            if (plugin.getConfig().contains("deepmine.bounds")) {
                startInstance(legacySpec());
            }
            return;
        }
        for (Map<?, ?> raw : rawInstances) {
            try {
                startInstance(InstanceSpec.fromMap(raw));
            } catch (Exception exception) {
                plugin.getLogger().warning("딥 마인 인스턴스 로드 실패: " + raw + " - " + exception.getMessage());
            }
        }
    }

    public void stop() {
        for (DeepMineManager manager : instances.values()) {
            manager.stop();
        }
    }

    public DeepMineManager get(String id) {
        return instances.get(id);
    }

    public List<DeepMineManager> getAll() {
        return new ArrayList<>(instances.values());
    }

    public DeepMineManager findContaining(Location location) {
        for (DeepMineManager manager : instances.values()) {
            if (manager.isInside(location)) {
                return manager;
            }
        }
        return null;
    }

    /** Nearest instance's entrance in the same world — used for organic portal discovery. */
    public DeepMineManager nearest(Location location) {
        DeepMineManager best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (DeepMineManager manager : instances.values()) {
            if (!manager.getWorldName().equals(location.getWorld().getName())) {
                continue;
            }
            double distanceSq = manager.getEntrance().distanceSquared(location);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = manager;
            }
        }
        return best;
    }

    /**
     * Automatically allocates a new non-overlapping bounding box (same size as
     * the first/template instance, offset along X) in the same world, starts
     * it, and appends it to config.yml so it survives a restart.
     */
    public DeepMineManager createAutoInstance(Player requester) {
        InstanceSpec template = instances.isEmpty() ? null : InstanceSpec.fromManager(instances.values().iterator().next());
        String world = template != null ? template.world : plugin.getConfig().getString("deepmine.world", "world");
        int width = template != null ? (template.maxX - template.minX) : 100;
        int height = template != null ? (template.maxY - template.minY) : 124;
        int depth = template != null ? (template.maxZ - template.minZ) : 100;
        long resetMinutes = template != null ? template.resetMinutes : plugin.getConfig().getLong("deepmine.reset-interval-minutes", 30);

        int nextIndex = instances.size() + 1;
        String id = "mine_" + nextIndex;
        while (instances.containsKey(id)) {
            nextIndex++;
            id = "mine_" + nextIndex;
        }

        // Find the rightmost (max X) edge among existing instances in the same world, then offset past it.
        int rightmostX = template != null ? template.maxX : 0;
        for (DeepMineManager manager : instances.values()) {
            if (manager.getWorldName().equals(world)) {
                rightmostX = Math.max(rightmostX, manager.getMaxX());
            }
        }

        int minX = rightmostX + INSTANCE_SPACING;
        int minY = template != null ? template.minY : -64;
        int minZ = template != null ? template.minZ : -200;
        int maxX = minX + width;
        int maxY = minY + height;
        int maxZ = minZ + depth;
        double entranceX = (minX + maxX) / 2.0;
        double entranceY = minY + height - 4;
        double entranceZ = (minZ + maxZ) / 2.0;

        InstanceSpec spec = new InstanceSpec(id, world, minX, minY, minZ, maxX, maxY, maxZ,
                entranceX, entranceY, entranceZ, resetMinutes);

        DeepMineManager manager = startInstance(spec);
        persist(spec);

        if (requester != null) {
            requester.sendMessage("§a새로운 딥 마인 인스턴스 §f'" + id + "'§a를 자동으로 생성했습니다.");
            requester.sendMessage("§7월드: §f" + world + " §7경계: §f(" + minX + "," + minY + "," + minZ
                    + ") ~ (" + maxX + "," + maxY + "," + maxZ + ")");
        }
        return manager;
    }

    private DeepMineManager startInstance(InstanceSpec spec) {
        World bukkitWorld = Bukkit.getWorld(spec.world);
        Location entrance = new Location(bukkitWorld, spec.entranceX, spec.entranceY, spec.entranceZ);
        DeepMineManager manager = new DeepMineManager(plugin, spec.id, spec.world,
                spec.minX, spec.minY, spec.minZ, spec.maxX, spec.maxY, spec.maxZ,
                entrance, spec.resetMinutes, itemFactory, runeItemFactory);
        manager.start();
        instances.put(spec.id, manager);
        return manager;
    }

    private InstanceSpec legacySpec() {
        String world = plugin.getConfig().getString("deepmine.world", "world");
        int minX = plugin.getConfig().getInt("deepmine.bounds.min.x");
        int minY = plugin.getConfig().getInt("deepmine.bounds.min.y");
        int minZ = plugin.getConfig().getInt("deepmine.bounds.min.z");
        int maxX = plugin.getConfig().getInt("deepmine.bounds.max.x");
        int maxY = plugin.getConfig().getInt("deepmine.bounds.max.y");
        int maxZ = plugin.getConfig().getInt("deepmine.bounds.max.z");
        double entranceX = plugin.getConfig().getDouble("deepmine.entrance.x");
        double entranceY = plugin.getConfig().getDouble("deepmine.entrance.y");
        double entranceZ = plugin.getConfig().getDouble("deepmine.entrance.z");
        long resetMinutes = plugin.getConfig().getLong("deepmine.reset-interval-minutes", 30);
        return new InstanceSpec("mine_1", world, minX, minY, minZ, maxX, maxY, maxZ,
                entranceX, entranceY, entranceZ, resetMinutes);
    }

    private void persist(InstanceSpec spec) {
        List<Map<?, ?>> rawInstances = new ArrayList<>(plugin.getConfig().getMapList("deepmine.instances"));
        if (rawInstances.isEmpty() && plugin.getConfig().contains("deepmine.bounds")) {
            // Migrate the legacy single-instance entry into the list so it isn't lost.
            rawInstances.add(legacySpec().toMap());
            plugin.getConfig().set("deepmine.world", null);
            plugin.getConfig().set("deepmine.bounds", null);
            plugin.getConfig().set("deepmine.entrance", null);
        }
        rawInstances.add(spec.toMap());
        plugin.getConfig().set("deepmine.instances", rawInstances);
        plugin.saveConfig();
    }

    private static final class InstanceSpec {
        final String id;
        final String world;
        final int minX, minY, minZ, maxX, maxY, maxZ;
        final double entranceX, entranceY, entranceZ;
        final long resetMinutes;

        InstanceSpec(String id, String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                     double entranceX, double entranceY, double entranceZ, long resetMinutes) {
            this.id = id;
            this.world = world;
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
            this.entranceX = entranceX;
            this.entranceY = entranceY;
            this.entranceZ = entranceZ;
            this.resetMinutes = resetMinutes;
        }

        static InstanceSpec fromMap(Map<?, ?> rawMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = (Map<String, Object>) rawMap;
            String id = String.valueOf(raw.get("id"));
            String world = raw.containsKey("world") ? String.valueOf(raw.get("world")) : "world";
            Map<?, ?> bounds = (Map<?, ?>) raw.get("bounds");
            Map<?, ?> min = (Map<?, ?>) bounds.get("min");
            Map<?, ?> max = (Map<?, ?>) bounds.get("max");
            Map<?, ?> entrance = (Map<?, ?>) raw.get("entrance");
            long resetMinutes = raw.containsKey("reset-interval-minutes")
                    ? ((Number) raw.get("reset-interval-minutes")).longValue() : 30L;
            return new InstanceSpec(id, world,
                    ((Number) min.get("x")).intValue(), ((Number) min.get("y")).intValue(), ((Number) min.get("z")).intValue(),
                    ((Number) max.get("x")).intValue(), ((Number) max.get("y")).intValue(), ((Number) max.get("z")).intValue(),
                    ((Number) entrance.get("x")).doubleValue(), ((Number) entrance.get("y")).doubleValue(), ((Number) entrance.get("z")).doubleValue(),
                    resetMinutes);
        }

        static InstanceSpec fromManager(DeepMineManager manager) {
            Location entrance = manager.getEntrance();
            return new InstanceSpec(manager.getId(), manager.getWorldName(),
                    manager.getMinX(), manager.getMinY(), manager.getMinZ(),
                    manager.getMaxX(), manager.getMaxY(), manager.getMaxZ(),
                    entrance.getX(), entrance.getY(), entrance.getZ(), 30);
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("world", world);
            Map<String, Object> bounds = new LinkedHashMap<>();
            Map<String, Object> minMap = new LinkedHashMap<>();
            minMap.put("x", minX);
            minMap.put("y", minY);
            minMap.put("z", minZ);
            Map<String, Object> maxMap = new LinkedHashMap<>();
            maxMap.put("x", maxX);
            maxMap.put("y", maxY);
            maxMap.put("z", maxZ);
            bounds.put("min", minMap);
            bounds.put("max", maxMap);
            map.put("bounds", bounds);
            Map<String, Object> entranceMap = new LinkedHashMap<>();
            entranceMap.put("x", entranceX);
            entranceMap.put("y", entranceY);
            entranceMap.put("z", entranceZ);
            map.put("entrance", entranceMap);
            map.put("reset-interval-minutes", resetMinutes);
            return map;
        }
    }
}
