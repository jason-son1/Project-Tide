package com.tide.rpg.deepmine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.tide.rpg.item.ItemFactory;
import com.tide.rpg.rune.RuneItemFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A self-contained "instance" carved out of one region instead of a full
 * world copy — periodically re-rolls its ore distribution rather than relying
 * on WorldEdit. Block writes are spread across many ticks so a 30-minute
 * reset never causes a single big lag spike.
 */
public final class DeepMineManager {

    private static final Material[] ORE_POOL = {
            Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE
    };
    private static final double[] ORE_WEIGHT = {0.35, 0.25, 0.15, 0.10, 0.08, 0.05, 0.02};
    private static final int BLOCKS_PER_TICK = 4000;

    private final JavaPlugin plugin;
    private final String worldName;
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    private final Location entrance;
    private final long resetIntervalTicks;
    private final ItemFactory itemFactory;
    private final RuneItemFactory runeItemFactory;

    private final Map<UUID, List<org.bukkit.inventory.ItemStack>> sessionLoot = new ConcurrentHashMap<>();
    private final Map<org.bukkit.block.Block, org.bukkit.entity.ArmorStand> activePortals = new ConcurrentHashMap<>();
    private final List<Location> chestLocations = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Location> trapChestLocations = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Location> eventLocations = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<SpawnerInfo> spawnerLocations = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Map<Location, String> spawnerMobs = new ConcurrentHashMap<>();
    private BukkitTask resetTask;
    private BukkitTask scheduleTask;
    private BukkitTask oxygenTask;
    private BukkitTask eventTask;

    public DeepMineManager(JavaPlugin plugin, String worldName, int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ, Location entrance, long resetIntervalMinutes,
                            ItemFactory itemFactory, RuneItemFactory runeItemFactory) {
        this.plugin = plugin;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.entrance = entrance;
        this.resetIntervalTicks = resetIntervalMinutes * 60L * 20L;
        this.itemFactory = itemFactory;
        this.runeItemFactory = runeItemFactory;
    }

    public void start() {
        scheduleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::reset, 20L, resetIntervalTicks);
        startOxygenTask();
        startEventTask();
    }

    public void stop() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
        }
        if (resetTask != null) {
            resetTask.cancel();
        }
        if (oxygenTask != null) {
            oxygenTask.cancel();
        }
        if (eventTask != null) {
            eventTask.cancel();
        }
        for (var entry : activePortals.entrySet()) {
            entry.getValue().remove();
            if (entry.getKey().getType() == Material.CRYING_OBSIDIAN) {
                entry.getKey().setType(Material.STONE);
            }
        }
        activePortals.clear();
    }

    private void startOxygenTask() {
        long intervalSeconds = plugin.getConfig().getLong("deepmine.toxic-gas-interval-seconds", 5);
        oxygenTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isInside(player.getLocation())) {
                    ItemStack helmet = player.getInventory().getHelmet();
                    boolean hasOxygen = false;
                    if (helmet != null && helmet.hasItemMeta()) {
                        var pdc = helmet.getItemMeta().getPersistentDataContainer();
                        NamespacedKey key = new NamespacedKey("tide", "oxygen_capacity");
                        if (pdc.has(key, PersistentDataType.INTEGER)) {
                            Integer cap = pdc.get(key, PersistentDataType.INTEGER);
                            if (cap != null && cap > 0) {
                                hasOxygen = true;
                            }
                        }
                    }

                    if (!hasOxygen) {
                        double dmg = plugin.getConfig().getDouble("deepmine.toxic-gas-damage", 2.0);
                        player.damage(dmg);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 1));
                        player.sendMessage("§c§l[위험] 유독 가스 노출! §7산소 장비가 없어 호흡이 곤란해집니다.");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
                        player.spawnParticle(Particle.DRAGON_BREATH, player.getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.05, 1.0f);
                    }
                }
            }
        }, 100L, intervalSeconds * 20L);
    }

    private void startEventTask() {
        // Trigger a scary event every 30-60 seconds to players inside
        eventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (eventLocations.isEmpty()) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isInside(player.getLocation())) {
                    if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
                    } else if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                        player.sendMessage("§8§o(멀리서 거대한 바위가 무너지는 소리가 들립니다...)");
                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.1f);
                    }
                }
            }
        }, 600L, 600L); // every 30 seconds
    }

    public Map<org.bukkit.block.Block, org.bukkit.entity.ArmorStand> getActivePortals() {
        return activePortals;
    }

    public void spawnPortalEntrance(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        org.bukkit.block.Block block = loc.getBlock();
        block.setType(Material.CRYING_OBSIDIAN);

        Location textLoc = loc.clone().add(0.5, 1.2, 0.5);
        ArmorStand stand = world.spawn(textLoc, ArmorStand.class, as -> {
            as.setGravity(false);
            as.setVisible(false);
            as.setCustomName("§d§l[심해 광산 통로] §7(우클릭하여 입장)");
            as.setCustomNameVisible(true);
            as.setMarker(true);
        });

        activePortals.put(block, stand);

        world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.2f);
        world.spawnParticle(Particle.PORTAL, loc.clone().add(0.5, 0.5, 0.5), 100, 0.5, 0.5, 0.5, 0.1);

        for (Player p : world.getPlayers()) {
            if (p.getLocation().distance(loc) <= 30) {
                p.sendMessage("§b[!] 근처의 지반이 흔들리며 심해 광산 통로(Crying Obsidian)가 드러났습니다!");
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.5f);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activePortals.containsKey(block)) {
                activePortals.remove(block);
                stand.remove();
                if (block.getType() == Material.CRYING_OBSIDIAN) {
                    block.setType(Material.STONE);
                }
                world.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
            }
        }, 1200L); // 60 seconds
    }

    public boolean isInside(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
            return false;
        }
        return location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockY() >= minY && location.getBlockY() <= maxY
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public void enter(Player player) {
        sessionLoot.putIfAbsent(player.getUniqueId(), new java.util.concurrent.CopyOnWriteArrayList<>());
        player.teleport(entrance);
        player.sendMessage("§b딥 마인에 입장했습니다. §7깊이 들어갈수록 위험하지만 보상도 커집니다.");
    }

    public void leave(Player player) {
        sessionLoot.remove(player.getUniqueId());
        player.teleport(entrance);
        player.sendMessage("§a딥 마인에서 안전하게 귀환했습니다. 획득한 전리품을 모두 유지합니다.");
    }

    public void trackLoot(Player player, org.bukkit.inventory.ItemStack itemStack) {
        sessionLoot.computeIfAbsent(player.getUniqueId(), uuid -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(itemStack.clone());
    }

    /** Called on death inside the mine: forces an exit and forfeits all of this session's loot. */
    public void onDeathInside(Player player) {
        List<org.bukkit.inventory.ItemStack> loot = sessionLoot.remove(player.getUniqueId());
        if (loot != null) {
            for (org.bukkit.inventory.ItemStack tracked : loot) {
                org.bukkit.inventory.ItemStack toRemove = tracked.clone();
                player.getInventory().removeItem(toRemove);
            }
        }
        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(entrance));
        player.sendMessage("§c광산에서 사망하여 강제로 추방되었습니다. 이 세션에서 획득한 모든 전리품을 잃었습니다!");
    }

    public String getSpawnerMobId(Location loc) {
        return spawnerMobs.get(loc);
    }

    public boolean isTrapChest(Location loc) {
        return trapChestLocations.contains(loc);
    }

    public List<Location> getEventLocations() {
        return eventLocations;
    }

    private org.bukkit.configuration.ConfigurationSection selectRandomTier() {
        var tiersSec = plugin.getConfig().getConfigurationSection("deepmine.tiers");
        if (tiersSec == null) return null;
        int totalWeight = 0;
        for (String key : tiersSec.getKeys(false)) {
            totalWeight += tiersSec.getInt(key + ".weight", 10);
        }
        if (totalWeight <= 0) return null;
        int r = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;
        for (String key : tiersSec.getKeys(false)) {
            currentWeight += tiersSec.getInt(key + ".weight", 10);
            if (r < currentWeight) {
                return tiersSec.getConfigurationSection(key);
            }
        }
        return tiersSec.getConfigurationSection(tiersSec.getKeys(false).iterator().next());
    }

    private void setBlock(Material[][][] blocks, int x, int y, int z, Material mat) {
        if (x >= 0 && x < blocks.length && y >= 0 && y < blocks[0].length && z >= 0 && z < blocks[0][0].length) {
            blocks[x][y][z] = mat;
        }
    }

    private void drawBox(Material[][][] blocks, int x1, int y1, int z1, int x2, int y2, int z2, Material mat) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    setBlock(blocks, x, y, z, mat);
                }
            }
        }
    }

    private void drawSupportBeam(Material[][][] blocks, int cx, int cz, int lyIdx, int height, boolean crossZAxis) {
        Material log = Material.STRIPPED_DARK_OAK_LOG;
        Material fence = Material.DARK_OAK_FENCE;

        if (crossZAxis) {
            for (int dy = 0; dy < height - 1; dy++) {
                setBlock(blocks, cx, lyIdx + dy, cz - 2, log);
                setBlock(blocks, cx, lyIdx + dy, cz + 2, log);
            }
            for (int dz = -2; dz <= 2; dz++) {
                setBlock(blocks, cx, lyIdx + height - 1, cz + dz, log);
            }
            setBlock(blocks, cx, lyIdx + height - 2, cz - 1, fence);
            setBlock(blocks, cx, lyIdx + height - 2, cz + 1, fence);
        } else {
            for (int dy = 0; dy < height - 1; dy++) {
                setBlock(blocks, cx - 2, lyIdx + dy, cz, log);
                setBlock(blocks, cx + 2, lyIdx + dy, cz, log);
            }
            for (int dx = -2; dx <= 2; dx++) {
                setBlock(blocks, cx + dx, lyIdx + height - 1, cz, log);
            }
            setBlock(blocks, cx - 1, lyIdx + height - 2, cz, fence);
            setBlock(blocks, cx + 1, lyIdx + height - 2, cz, fence);
        }
    }

    private Material rollCorridorFloor(int realY) {
        double r = ThreadLocalRandom.current().nextDouble();
        if (realY < 0) {
            if (r < 0.40) return Material.GRAVEL;
            if (r < 0.70) return Material.COBBLED_DEEPSLATE;
            if (r < 0.85) return Material.POLISHED_TUFF;
            return Material.TUFF;
        } else {
            if (r < 0.40) return Material.GRAVEL;
            if (r < 0.70) return Material.COBBLESTONE;
            if (r < 0.85) return Material.MOSSY_COBBLESTONE;
            return Material.ANDESITE;
        }
    }

    private void carveBaseRoom(Material[][][] blocks, int cx, int cz, int lyIdx, int rx, int rz, int rh, boolean isDeep) {
        Material wallMat = isDeep ? Material.DEEPSLATE_BRICKS : Material.STONE_BRICKS;
        Material floorMat1 = isDeep ? Material.POLISHED_DEEPSLATE : Material.POLISHED_ANDESITE;
        Material floorMat2 = isDeep ? Material.DARK_PRISMARINE : Material.PRISMARINE_BRICKS;
        
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = 0; dy < rh; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    int lx = cx + dx;
                    int ly = lyIdx + dy;
                    int lz = cz + dz;
                    
                    if (dy == 0) {
                        if ((dx + dz) % 2 == 0) {
                            setBlock(blocks, lx, ly, lz, floorMat1);
                        } else {
                            setBlock(blocks, lx, ly, lz, floorMat2);
                        }
                    } else if (dy == rh - 1) {
                        setBlock(blocks, lx, ly, lz, wallMat);
                    } else {
                        if (dx == -rx || dx == rx || dz == -rz || dz == rz) {
                            setBlock(blocks, lx, ly, lz, wallMat);
                        } else {
                            setBlock(blocks, lx, ly, lz, Material.AIR);
                        }
                    }
                }
            }
        }
        
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                if (Math.abs(dx) == rx - 1 || Math.abs(dz) == rz - 1) {
                    setBlock(blocks, cx + dx, lyIdx + rh - 2, cz + dz, wallMat);
                }
            }
        }

        Material pillarMat = isDeep ? Material.CHISELED_DEEPSLATE : Material.CHISELED_STONE_BRICKS;
        Material bulbMat = isDeep ? Material.COPPER_BULB : Material.SOUL_LANTERN;
        
        for (int dy = 1; dy < rh - 1; dy++) {
            for (int dx = -rx + 2; dx <= rx - 2; dx += 3) {
                setBlock(blocks, cx + dx, lyIdx + dy, cz - rz, pillarMat);
                setBlock(blocks, cx + dx, lyIdx + dy, cz + rz, pillarMat);
                if (dy == rh - 3) {
                    setBlock(blocks, cx + dx, lyIdx + dy, cz - rz + 1, bulbMat);
                    setBlock(blocks, cx + dx, lyIdx + dy, cz + rz - 1, bulbMat);
                }
            }
            for (int dz = -rz + 2; dz <= rz - 2; dz += 3) {
                setBlock(blocks, cx - rx, lyIdx + dy, cz + dz, pillarMat);
                setBlock(blocks, cx + rx, lyIdx + dy, cz + dz, pillarMat);
                if (dy == rh - 3) {
                    setBlock(blocks, cx - rx + 1, lyIdx + dy, cz + dz, bulbMat);
                    setBlock(blocks, cx + rx - 1, lyIdx + dy, cz + dz, bulbMat);
                }
            }
        }
    }

    private Material rollBaseOre(boolean isDeep) {
        double r = ThreadLocalRandom.current().nextDouble();
        if (isDeep) {
            if (r < 0.40) return Material.RAW_IRON_BLOCK;
            if (r < 0.70) return Material.RAW_COPPER_BLOCK;
            return Material.COAL_ORE;
        } else {
            if (r < 0.40) return Material.RAW_IRON_BLOCK;
            if (r < 0.70) return Material.RAW_COPPER_BLOCK;
            return Material.COAL_ORE;
        }
    }

    private Material rollMediumOre(boolean isDeep) {
        double r = ThreadLocalRandom.current().nextDouble();
        if (isDeep) {
            if (r < 0.40) return Material.DEEPSLATE_GOLD_ORE;
            if (r < 0.70) return Material.DEEPSLATE_REDSTONE_ORE;
            return Material.RAW_GOLD_BLOCK;
        } else {
            if (r < 0.40) return Material.GOLD_ORE;
            if (r < 0.70) return Material.REDSTONE_ORE;
            return Material.RAW_GOLD_BLOCK;
        }
    }

    private Material rollRareOre(boolean isDeep) {
        double r = ThreadLocalRandom.current().nextDouble();
        if (isDeep) {
            if (r < 0.50) return Material.DEEPSLATE_DIAMOND_ORE;
            return Material.DEEPSLATE_EMERALD_ORE;
        } else {
            if (r < 0.50) return Material.DIAMOND_ORE;
            return Material.EMERALD_ORE;
        }
    }

    private void reset() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }

        if (resetTask != null) {
            resetTask.cancel();
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;

        Material[][][] blocks = new Material[width][height][depth];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int realY = minY + y;
                Material defaultBlock;
                if (realY < -60) {
                    defaultBlock = Material.BEDROCK;
                } else if (realY < 0) {
                    double r = ThreadLocalRandom.current().nextDouble();
                    defaultBlock = r < 0.88 ? Material.DEEPSLATE : (r < 0.98 ? Material.TUFF : Material.SMOOTH_BASALT);
                } else {
                    double r = ThreadLocalRandom.current().nextDouble();
                    defaultBlock = r < 0.90 ? Material.STONE : (r < 0.97 ? Material.ANDESITE : Material.DIORITE);
                }
                for (int z = 0; z < depth; z++) {
                    blocks[x][y][z] = defaultBlock;
                }
            }
        }

        chestLocations.clear();
        trapChestLocations.clear();
        spawnerLocations.clear();
        spawnerMobs.clear();
        eventLocations.clear();

        org.bukkit.configuration.ConfigurationSection tier = selectRandomTier();
        int gridSize = 3;
        List<Integer> layersYList = List.of(-50, -20, 10, 40);
        double pEmpty = 0.1, pPillar = 0.2, pTreasure = 0.2, pMonster = 0.2, pOreVault = 0.3, pTrap = 0.0, pParkour = 0.0, pMiniboss = 0.0;
        
        if (tier != null) {
            gridSize = tier.getInt("grid-size", 3);
            layersYList = tier.getIntegerList("layers");
            if (layersYList.isEmpty()) layersYList = List.of(-50, -20, 10, 40);
            
            pEmpty = tier.getDouble("room-styles.empty", 0.1);
            pPillar = tier.getDouble("room-styles.pillar", 0.3);
            pTreasure = tier.getDouble("room-styles.treasure", 0.2);
            pMonster = tier.getDouble("room-styles.monster", 0.2);
            pOreVault = tier.getDouble("room-styles.ore_vault", 0.2);
            pTrap = tier.getDouble("room-styles.trap", 0.0);
            pParkour = tier.getDouble("room-styles.parkour", 0.0);
            pMiniboss = tier.getDouble("room-styles.miniboss", 0.0);
        }

        int entXIdx = entrance.getBlockX() - minX;
        int entYIdx = entrance.getBlockY() - minY;
        int entZIdx = entrance.getBlockZ() - minZ;

        int lobbyRadius = 4;
        for (int dx = -lobbyRadius; dx <= lobbyRadius; dx++) {
            for (int dy = -1; dy <= 4; dy++) {
                for (int dz = -lobbyRadius; dz <= lobbyRadius; dz++) {
                    int lx = entXIdx + dx;
                    int ly = entYIdx + dy;
                    int lz = entZIdx + dz;
                    
                    if (lx >= 0 && lx < width && ly >= 0 && ly < height && lz >= 0 && lz < depth) {
                        if (dy == -1) {
                            if ((dx + dz) % 2 == 0) {
                                blocks[lx][ly][lz] = Material.DARK_PRISMARINE;
                            } else {
                                blocks[lx][ly][lz] = Material.COPPER_GRATE;
                            }
                        } else if (dy == 4) {
                            blocks[lx][ly][lz] = Material.DEEPSLATE_BRICKS;
                        } else {
                            if (dx == -lobbyRadius || dx == lobbyRadius || dz == -lobbyRadius || dz == lobbyRadius) {
                                blocks[lx][ly][lz] = Material.DEEPSLATE_BRICKS;
                                if (dy == 2 && (dx == 0 || dz == 0)) {
                                    blocks[lx][ly][lz] = Material.COPPER_BULB;
                                }
                            } else {
                                blocks[lx][ly][lz] = Material.AIR;
                            }
                        }
                    }
                }
            }
        }
        
        for (int dy = 0; dy < 4; dy++) {
            setBlock(blocks, entXIdx - 3, entYIdx + dy, entZIdx - 3, Material.STRIPPED_DARK_OAK_LOG);
            setBlock(blocks, entXIdx - 3, entYIdx + dy, entZIdx + 3, Material.STRIPPED_DARK_OAK_LOG);
            setBlock(blocks, entXIdx + 3, entYIdx + dy, entZIdx - 3, Material.STRIPPED_DARK_OAK_LOG);
            setBlock(blocks, entXIdx + 3, entYIdx + dy, entZIdx + 3, Material.STRIPPED_DARK_OAK_LOG);
        }

        int[] layersY = layersYList.stream().mapToInt(Integer::intValue).toArray();

        int[] cellCentersX = new int[gridSize];
        int[] cellCentersZ = new int[gridSize];
        int usableWidth = width - 20;
        int usableDepth = depth - 20;
        int stepX = usableWidth / gridSize;
        int stepZ = usableDepth / gridSize;
        for (int i = 0; i < gridSize; i++) {
            cellCentersX[i] = 10 + (stepX / 2) + (i * stepX);
            cellCentersZ[i] = 10 + (stepZ / 2) + (i * stepZ);
        }

        for (int layer = 0; layer < layersY.length; layer++) {
            int realLayerY = layersY[layer];
            int layerYIdx = realLayerY - minY;
            if (layerYIdx < 0 || layerYIdx >= height) continue;

            boolean[][] hasRoom = new boolean[gridSize][gridSize];
            boolean isDeep = realLayerY < 0;

            for (int r = 0; r < gridSize; r++) {
                for (int c = 0; c < gridSize; c++) {
                    if (ThreadLocalRandom.current().nextDouble() < 0.85) {
                        hasRoom[r][c] = true;
                        int cx = cellCentersX[c];
                        int cz = cellCentersZ[r];

                        int rx = ThreadLocalRandom.current().nextInt(5, Math.max(6, (stepX/2) - 1));
                        int rz = ThreadLocalRandom.current().nextInt(5, Math.max(6, (stepZ/2) - 1));
                        int rh = ThreadLocalRandom.current().nextInt(5, 7);

                        double roll = ThreadLocalRandom.current().nextDouble();
                        int roomStyle = 0;
                        double cumulative = pEmpty;
                        if (roll < cumulative) roomStyle = 0;
                        else if (roll < (cumulative += pPillar)) roomStyle = 1;
                        else if (roll < (cumulative += pTreasure)) roomStyle = 2;
                        else if (roll < (cumulative += pMonster)) roomStyle = 3;
                        else if (roll < (cumulative += pOreVault)) roomStyle = 4;
                        else if (roll < (cumulative += pTrap)) roomStyle = 5;
                        else if (roll < (cumulative += pParkour)) roomStyle = 6;
                        else roomStyle = 7;

                        carveBaseRoom(blocks, cx, cz, layerYIdx, rx, rz, rh, isDeep);

                        if (roomStyle == 1) {
                            int py = layerYIdx + 1;
                            Material obeliskBase = isDeep ? Material.POLISHED_DEEPSLATE : Material.POLISHED_ANDESITE;
                            Material obeliskCore = isDeep ? Material.DARK_PRISMARINE : Material.PRISMARINE_BRICKS;
                            Material obeliskGlow = isDeep ? Material.SEA_LANTERN : Material.COPPER_BULB;
                            
                            for (int dy = 0; dy < rh - 2; dy++) {
                                for (int dx = -1; dx <= 1; dx++) {
                                    for (int dz = -1; dz <= 1; dz++) {
                                        if (dx == 0 && dz == 0) {
                                            setBlock(blocks, cx + dx, py + dy, cz + dz, obeliskGlow);
                                        } else {
                                            if (dy == 0 || dy == rh - 3) {
                                                setBlock(blocks, cx + dx, py + dy, cz + dz, obeliskBase);
                                            } else {
                                                setBlock(blocks, cx + dx, py + dy, cz + dz, obeliskCore);
                                            }
                                        }
                                    }
                                }
                            }
                            setBlock(blocks, cx - 1, py + rh - 3, cz - 1, Material.CHAIN);
                            setBlock(blocks, cx - 1, py + rh - 3, cz + 1, Material.CHAIN);
                            setBlock(blocks, cx + 1, py + rh - 3, cz - 1, Material.CHAIN);
                            setBlock(blocks, cx + 1, py + rh - 3, cz + 1, Material.CHAIN);
                            
                        } else if (roomStyle == 2) {
                            for (int dx = -2; dx <= 2; dx++) {
                                for (int dz = -2; dz <= 2; dz++) {
                                    setBlock(blocks, cx + dx, layerYIdx, cz + dz, Material.WATER);
                                }
                            }
                            setBlock(blocks, cx, layerYIdx, cz, Material.GOLD_BLOCK);
                            setBlock(blocks, cx, layerYIdx + 1, cz, isDeep ? Material.CHISELED_DEEPSLATE : Material.CHISELED_STONE_BRICKS);
                            
                            setBlock(blocks, cx, layerYIdx + 2, cz, Material.CHEST);
                            chestLocations.add(new Location(world, minX + cx, realLayerY + 2, minZ + cz));
                            
                            for (int dx = -3; dx <= 3; dx++) {
                                setBlock(blocks, cx + dx, layerYIdx + 1, cz - 3, Material.IRON_BARS);
                                setBlock(blocks, cx + dx, layerYIdx + 1, cz + 3, Material.IRON_BARS);
                            }
                            for (int dz = -3; dz <= 3; dz++) {
                                setBlock(blocks, cx - 3, layerYIdx + 1, cz + dz, Material.IRON_BARS);
                                setBlock(blocks, cx + 3, layerYIdx + 1, cz + dz, Material.IRON_BARS);
                            }
                            
                        } else if (roomStyle == 3) {
                            int sy = layerYIdx + 3;
                            if (sy < layerYIdx + rh - 1) {
                                for (int y = sy + 1; y < layerYIdx + rh - 1; y++) {
                                    setBlock(blocks, cx, y, cz, Material.CHAIN);
                                }
                                setBlock(blocks, cx, sy, cz, Material.SPAWNER);
                                String mobId = rollDungeonMob(realLayerY);
                                Location spawnerLoc = new Location(world, minX + cx, realLayerY + sy, minZ + cz);
                                spawnerLocations.add(new SpawnerInfo(spawnerLoc, mobId));
                                spawnerMobs.put(spawnerLoc, mobId);
                                
                                setBlock(blocks, cx - 1, sy, cz, Material.IRON_BARS);
                                setBlock(blocks, cx + 1, sy, cz, Material.IRON_BARS);
                                setBlock(blocks, cx, sy, cz - 1, Material.IRON_BARS);
                                setBlock(blocks, cx, sy, cz + 1, Material.IRON_BARS);
                            }
                            
                            for (int dx = -rx + 1; dx <= rx - 1; dx++) {
                                for (int dz = -rz + 1; dz <= rz - 1; dz++) {
                                    double randVal = ThreadLocalRandom.current().nextDouble();
                                    if (randVal < 0.05) {
                                        setBlock(blocks, cx + dx, layerYIdx + 1, cz + dz, Material.BONE_BLOCK);
                                    } else if (randVal < 0.10) {
                                        setBlock(blocks, cx + dx, layerYIdx + 1, cz + dz, isDeep ? Material.COBBLED_DEEPSLATE : Material.COBBLESTONE);
                                    }
                                }
                            }
                            eventLocations.add(new Location(world, minX + cx, realLayerY + 2, minZ + cz));
                            
                        } else if (roomStyle == 4) {
                            for (int dx = -2; dx <= 2; dx++) {
                                for (int dz = -2; dz <= 2; dz++) {
                                    setBlock(blocks, cx + dx, layerYIdx + 1, cz + dz, rollBaseOre(isDeep));
                                }
                            }
                            for (int dx = -1; dx <= 1; dx++) {
                                for (int dz = -1; dz <= 1; dz++) {
                                    setBlock(blocks, cx + dx, layerYIdx + 2, cz + dz, rollMediumOre(isDeep));
                                }
                            }
                            setBlock(blocks, cx, layerYIdx + 3, cz, rollRareOre(isDeep));
                            
                            Material fenceMat = Material.DARK_OAK_FENCE;
                            setBlock(blocks, cx - 3, layerYIdx + 1, cz - 3, fenceMat);
                            setBlock(blocks, cx - 3, layerYIdx + 2, cz - 3, fenceMat);
                            setBlock(blocks, cx - 3, layerYIdx + 1, cz + 3, fenceMat);
                            setBlock(blocks, cx - 3, layerYIdx + 2, cz + 3, fenceMat);
                            setBlock(blocks, cx + 3, layerYIdx + 1, cz - 3, fenceMat);
                            setBlock(blocks, cx + 3, layerYIdx + 2, cz - 3, fenceMat);
                            setBlock(blocks, cx + 3, layerYIdx + 1, cz + 3, fenceMat);
                            setBlock(blocks, cx + 3, layerYIdx + 2, cz + 3, fenceMat);
                            
                        } else if (roomStyle == 5) {
                            setBlock(blocks, cx, layerYIdx + 1, cz, Material.TRAPPED_CHEST);
                            trapChestLocations.add(new Location(world, minX + cx, realLayerY + 1, minZ + cz));
                            
                            for (int dx = -rx + 1; dx <= rx - 1; dx++) {
                                for (int dz = -rz + 1; dz <= rz - 1; dz++) {
                                    if (dx == 0 && dz == 0) continue;
                                    double randVal = ThreadLocalRandom.current().nextDouble();
                                    if (randVal < 0.15) {
                                        setBlock(blocks, cx + dx, layerYIdx + 1, cz + dz, Material.COBWEB);
                                    } else if (randVal < 0.08) {
                                        setBlock(blocks, cx + dx, layerYIdx + 1, cz + dz, Material.OAK_PRESSURE_PLATE);
                                        if (layerYIdx + 4 < layerYIdx + rh - 1) {
                                            setBlock(blocks, cx + dx, layerYIdx + 4, cz + dz, Material.GRAVEL);
                                            setBlock(blocks, cx + dx, layerYIdx + 2, cz + dz, Material.AIR);
                                            setBlock(blocks, cx + dx, layerYIdx + 3, cz + dz, Material.AIR);
                                        }
                                    }
                                }
                            }
                            
                        } else if (roomStyle == 6) {
                            Material hazardMat = (realLayerY < -30) ? Material.LAVA : Material.WATER;
                            for (int dx = -rx + 1; dx <= rx - 1; dx++) {
                                for (int dz = -rz + 1; dz <= rz - 1; dz++) {
                                    setBlock(blocks, cx + dx, layerYIdx, cz + dz, hazardMat);
                                }
                            }
                            
                            int platX = cx + rx - 2;
                            int platZ = cz + rz - 2;
                            setBlock(blocks, platX, layerYIdx, platZ, Material.DEEPSLATE_BRICKS);
                            setBlock(blocks, platX, layerYIdx + 1, platZ, Material.DEEPSLATE_BRICKS);
                            setBlock(blocks, platX, layerYIdx + 2, platZ, Material.CHEST);
                            chestLocations.add(new Location(world, minX + platX, realLayerY + 2, minZ + platZ));
                            
                            for (int dx = -rx + 2; dx <= rx - 2; dx += 2) {
                                for (int dz = -rz + 2; dz <= rz - 2; dz += 2) {
                                    if (dx == platX - cx && dz == platZ - cz) continue;
                                    double randVal = ThreadLocalRandom.current().nextDouble();
                                    if (randVal < 0.30) {
                                        setBlock(blocks, cx + dx, layerYIdx, cz + dz, Material.BASALT);
                                        setBlock(blocks, cx + dx, layerYIdx + 1, cz + dz, Material.POLISHED_BASALT);
                                    } else if (randVal < 0.50) {
                                        setBlock(blocks, cx + dx, layerYIdx, cz + dz, Material.DEEPSLATE_BRICKS);
                                        setBlock(blocks, cx + dx, layerYIdx + 1, cz + dz, (hazardMat == Material.LAVA) ? Material.MAGMA_BLOCK : Material.DEEPSLATE_TILES);
                                    } else if (randVal < 0.70) {
                                        setBlock(blocks, cx + dx, layerYIdx + 3, cz + dz, Material.CHAIN);
                                        setBlock(blocks, cx + dx, layerYIdx + 2, cz + dz, Material.COBBLED_DEEPSLATE_WALL);
                                    }
                                }
                            }
                            
                        } else if (roomStyle == 7) {
                            for (int dx = -3; dx <= 3; dx++) {
                                for (int dz = -3; dz <= 3; dz++) {
                                    double dist = Math.sqrt(dx*dx + dz*dz);
                                    if (dist <= 3.2) {
                                        setBlock(blocks, cx + dx, layerYIdx, cz + dz, Material.WARPED_PLANKS);
                                        if (dist <= 1.5) {
                                            setBlock(blocks, cx + dx, layerYIdx, cz + dz, Material.SEA_LANTERN);
                                        }
                                    }
                                }
                            }
                            
                            for (int dy = 1; dy < rh - 1; dy++) {
                                setBlock(blocks, cx - 3, layerYIdx + dy, cz - 3, Material.CRYING_OBSIDIAN);
                                setBlock(blocks, cx - 3, layerYIdx + dy, cz + 3, Material.CRYING_OBSIDIAN);
                                setBlock(blocks, cx + 3, layerYIdx + dy, cz - 3, Material.CRYING_OBSIDIAN);
                                setBlock(blocks, cx + 3, layerYIdx + dy, cz + 3, Material.CRYING_OBSIDIAN);
                            }
                            
                            setBlock(blocks, cx, layerYIdx + 1, cz, Material.SPAWNER);
                            String bossMobId = (realLayerY < -30) ? "abyssal_guardian" : "abyss_phantom";
                            Location spawnerLoc = new Location(world, minX + cx, realLayerY + 1, minZ + cz);
                            spawnerLocations.add(new SpawnerInfo(spawnerLoc, bossMobId));
                            spawnerMobs.put(spawnerLoc, bossMobId);
                            
                            int ty = layerYIdx + 1;
                            int tz = cz - rz + 2;
                            setBlock(blocks, cx - 1, ty, tz, Material.POLISHED_DEEPSLATE_WALL);
                            setBlock(blocks, cx + 1, ty, tz, Material.POLISHED_DEEPSLATE_WALL);
                            setBlock(blocks, cx - 1, ty + 1, tz, Material.POLISHED_DEEPSLATE_WALL);
                            setBlock(blocks, cx + 1, ty + 1, tz, Material.POLISHED_DEEPSLATE_WALL);
                            setBlock(blocks, cx, ty, tz, Material.POLISHED_DEEPSLATE);
                            setBlock(blocks, cx, ty + 1, tz, Material.POLISHED_DEEPSLATE_WALL);
                            
                            setBlock(blocks, cx, ty, tz + 1, Material.ENDER_CHEST);
                            chestLocations.add(new Location(world, minX + cx, realLayerY + ty, minZ + (tz + 1)));
                        }
                    }
                }
            }

            for (int r = 0; r < gridSize; r++) {
                for (int c = 0; c < gridSize; c++) {
                    if (hasRoom[r][c]) {
                        int cx = cellCentersX[c];
                        int cz = cellCentersZ[r];

                        if (c + 1 < gridSize && hasRoom[r][c + 1]) {
                            int nextCx = cellCentersX[c + 1];
                            for (int x = cx; x <= nextCx; x++) {
                                boolean isBeam = (x - cx) > 1 && (nextCx - x) > 1 && (x - cx) % 5 == 2;
                                for (int dy = 0; dy < 4; dy++) {
                                    int ly = layerYIdx + dy;
                                    for (int dz = -2; dz <= 2; dz++) {
                                        int lz = cz + dz;
                                        if (dy == 0) {
                                            setBlock(blocks, x, ly, lz, rollCorridorFloor(realLayerY));
                                        } else if (dy == 3) {
                                            if (dz == -2 || dz == 2) {
                                                setBlock(blocks, x, ly, lz, isDeep ? Material.DEEPSLATE_BRICKS : Material.STONE_BRICKS);
                                            } else {
                                                setBlock(blocks, x, ly, lz, Material.AIR);
                                                if (isBeam && dz == 0) {
                                                    setBlock(blocks, x, ly, lz, Material.COPPER_BULB);
                                                }
                                            }
                                        } else {
                                            if (dz == -2 || dz == 2) {
                                                setBlock(blocks, x, ly, lz, isDeep ? Material.DEEPSLATE_BRICKS : Material.STONE_BRICKS);
                                            } else {
                                                setBlock(blocks, x, ly, lz, Material.AIR);
                                            }
                                        }
                                    }
                                }
                                if (isBeam) {
                                    drawSupportBeam(blocks, x, cz, layerYIdx, 4, true);
                                }
                                double decorRoll = ThreadLocalRandom.current().nextDouble();
                                if (decorRoll < 0.12) {
                                    if (!isBeam) {
                                        setBlock(blocks, x, layerYIdx + 1, cz, Material.RAIL);
                                    }
                                } else if (decorRoll < 0.18) {
                                    setBlock(blocks, x, layerYIdx + 1, cz - 2, rollOre(realLayerY));
                                    setBlock(blocks, x, layerYIdx + 2, cz - 2, Material.GLOW_LICHEN);
                                } else if (decorRoll < 0.22) {
                                    setBlock(blocks, x, layerYIdx + 1, cz + 2, rollOre(realLayerY));
                                    setBlock(blocks, x, layerYIdx + 2, cz + 2, Material.GLOW_LICHEN);
                                } else if (decorRoll < 0.26) {
                                    if (!isBeam) {
                                        setBlock(blocks, x, layerYIdx + 1, cz + 1, Material.BARREL);
                                    }
                                }
                            }
                        }

                        if (r + 1 < gridSize && hasRoom[r + 1][c]) {
                            int nextCz = cellCentersZ[r + 1];
                            for (int z = cz; z <= nextCz; z++) {
                                boolean isBeam = (z - cz) > 1 && (nextCz - z) > 1 && (z - cz) % 5 == 2;
                                for (int dy = 0; dy < 4; dy++) {
                                    int ly = layerYIdx + dy;
                                    for (int dx = -2; dx <= 2; dx++) {
                                        int lx = cx + dx;
                                        if (dy == 0) {
                                            setBlock(blocks, lx, ly, z, rollCorridorFloor(realLayerY));
                                        } else if (dy == 3) {
                                            if (dx == -2 || dx == 2) {
                                                setBlock(blocks, lx, ly, z, isDeep ? Material.DEEPSLATE_BRICKS : Material.STONE_BRICKS);
                                            } else {
                                                setBlock(blocks, lx, ly, z, Material.AIR);
                                                if (isBeam && dx == 0) {
                                                    setBlock(blocks, lx, ly, z, Material.COPPER_BULB);
                                                }
                                            }
                                        } else {
                                            if (dx == -2 || dx == 2) {
                                                setBlock(blocks, lx, ly, z, isDeep ? Material.DEEPSLATE_BRICKS : Material.STONE_BRICKS);
                                            } else {
                                                setBlock(blocks, lx, ly, z, Material.AIR);
                                            }
                                        }
                                    }
                                }
                                if (isBeam) {
                                    drawSupportBeam(blocks, cx, z, layerYIdx, 4, false);
                                }
                                double decorRoll = ThreadLocalRandom.current().nextDouble();
                                if (decorRoll < 0.12) {
                                    if (!isBeam) {
                                        setBlock(blocks, cx, layerYIdx + 1, z, Material.RAIL);
                                    }
                                } else if (decorRoll < 0.18) {
                                    setBlock(blocks, cx - 2, layerYIdx + 1, z, rollOre(realLayerY));
                                    setBlock(blocks, cx - 2, layerYIdx + 2, z, Material.GLOW_LICHEN);
                                } else if (decorRoll < 0.22) {
                                    setBlock(blocks, cx + 2, layerYIdx + 1, z, rollOre(realLayerY));
                                    setBlock(blocks, cx + 2, layerYIdx + 2, z, Material.GLOW_LICHEN);
                                } else if (decorRoll < 0.26) {
                                    if (!isBeam) {
                                        setBlock(blocks, cx + 1, layerYIdx + 1, z, Material.BARREL);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (layer > 0) {
                int sr = -1, sc = -1;
                for (int r = 0; r < gridSize; r++) {
                    for (int c = 0; c < gridSize; c++) {
                        if (hasRoom[r][c]) {
                            sr = r;
                            sc = c;
                            break;
                        }
                    }
                    if (sr != -1) break;
                }

                if (sr != -1) {
                    int cx = cellCentersX[sc];
                    int cz = cellCentersZ[sr];
                    int belowY = layersY[layer - 1];
                    int belowYIdx = belowY - minY;

                    for (int y = belowYIdx; y <= layerYIdx; y++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                int lx = cx + dx;
                                int ly = y;
                                int lz = cz + dz;
                                
                                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                                    setBlock(blocks, lx, ly, lz, Material.STRIPPED_DARK_OAK_LOG);
                                    continue;
                                }
                                
                                if (y == belowYIdx) {
                                    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                                        setBlock(blocks, lx, ly, lz, Material.WATER);
                                    } else {
                                        setBlock(blocks, lx, ly, lz, Material.POLISHED_DEEPSLATE);
                                    }
                                } else {
                                    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                                        setBlock(blocks, lx, ly, lz, Material.AIR);
                                        if (dz == -1 && dx == 0) {
                                            setBlock(blocks, lx, ly, lz, Material.LADDER);
                                        }
                                    } else {
                                        if (dx == -2 || dx == 2 || dz == -2 || dz == 2) {
                                            setBlock(blocks, lx, ly, lz, Material.IRON_BARS);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        int topLayerYIdx = layersY[layersY.length - 1] - minY;
        for (int y = topLayerYIdx; y <= entYIdx; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int lx = entXIdx + dx;
                    int ly = y;
                    int lz = entZIdx + dz;
                    
                    if (lx >= 0 && lx < width && ly >= 0 && ly < height && lz >= 0 && lz < depth) {
                        if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                            blocks[lx][ly][lz] = Material.STRIPPED_DARK_OAK_LOG;
                            continue;
                        }
                        
                        if (y == topLayerYIdx) {
                            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                                blocks[lx][ly][lz] = Material.WATER;
                            } else {
                                blocks[lx][ly][lz] = Material.POLISHED_DEEPSLATE;
                            }
                        } else {
                            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                                blocks[lx][ly][lz] = Material.AIR;
                                if (dz == -1 && dx == 0) {
                                    blocks[lx][ly][lz] = Material.LADDER;
                                }
                            } else {
                                if (dx == -2 || dx == 2 || dz == -2 || dz == 2) {
                                    blocks[lx][ly][lz] = Material.IRON_BARS;
                                }
                            }
                        }
                    }
                }
            }
        }

        final int finalHeight = height;
        final int finalWidth = width;
        final int finalDepth = depth;
        final Material[][][] finalBlocks = blocks;

        plugin.getLogger().info("딥 마인 던전 리셋 시작: " + (width * height * depth) + "개 블록");

        final int[] curX = {0};
        final int[] curY = {0};
        final int[] curZ = {0};

        resetTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int processed = 0;
            while (processed < BLOCKS_PER_TICK) {
                if (curY[0] >= finalHeight) {
                    resetTask.cancel();
                    completeReset();
                    break;
                }

                int rx = minX + curX[0];
                int ry = minY + curY[0];
                int rz = minZ + curZ[0];

                Material mat = finalBlocks[curX[0]][curY[0]][curZ[0]];
                org.bukkit.block.Block b = world.getBlockAt(rx, ry, rz);
                b.setType(mat, false);
                if (mat == Material.LADDER) {
                    if (b.getBlockData() instanceof org.bukkit.block.data.type.Ladder ladder) {
                        ladder.setFacing(org.bukkit.block.BlockFace.SOUTH);
                        b.setBlockData(ladder, false);
                    }
                }

                processed++;
                curX[0]++;
                if (curX[0] >= finalWidth) {
                    curX[0] = 0;
                    curZ[0]++;
                    if (curZ[0] >= finalDepth) {
                        curZ[0] = 0;
                        curY[0]++;
                    }
                }
            }
        }, 0L, 1L);
    }


    private void completeReset() {
        // 1. Populate chests
        for (Location loc : chestLocations) {
            org.bukkit.block.Block block = loc.getBlock();
            if (block.getType() == Material.CHEST || block.getType() == Material.BARREL) {
                org.bukkit.block.BlockState state = block.getState();
                if (state instanceof org.bukkit.block.Chest chest) {
                    populateChestLoot(chest.getInventory());
                } else if (state instanceof org.bukkit.block.Container container) {
                    populateChestLoot(container.getInventory());
                }
            }
        }

        // 2. Setup spawners
        for (SpawnerInfo info : spawnerLocations) {
            org.bukkit.block.Block block = info.loc.getBlock();
            if (block.getType() == Material.SPAWNER) {
                org.bukkit.block.BlockState state = block.getState();
                if (state instanceof org.bukkit.block.CreatureSpawner spawner) {
                    spawner.setSpawnedType(getBaseEntityType(info.mobId));
                    spawner.update(true, false);
                }
            }
        }

        chestLocations.clear();
        spawnerLocations.clear();

        // 3. Teleport players inside to entrance lobby
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            for (Player player : world.getPlayers()) {
                if (isInside(player.getLocation())) {
                    if (player.getLocation().distance(entrance) > 10) {
                        player.teleport(entrance);
                        player.sendMessage("§d§l[!] 심해 광산이 무너지며 새로운 던전 형태로 리셋되었습니다!");
                        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.5f);
                    }
                }
            }
        }

        plugin.getLogger().info("딥 마인 던전 리셋 완료.");
    }

    private String rollDungeonMob(int y) {
        double r = ThreadLocalRandom.current().nextDouble();
        if (y < -20) {
            if (r < 0.25) return "mine_saboteur_creeper";
            if (r < 0.50) return "abyssal_husk";
            if (r < 0.75) return "brine_spider";
            return "rustfang_skeleton";
        } else {
            if (r < 0.50) return "abyssal_husk";
            return "brine_spider";
        }
    }

    private org.bukkit.entity.EntityType getBaseEntityType(String mobId) {
        return switch (mobId) {
            case "mine_saboteur_creeper" -> org.bukkit.entity.EntityType.CREEPER;
            case "abyssal_husk" -> org.bukkit.entity.EntityType.ZOMBIE;
            case "brine_spider" -> org.bukkit.entity.EntityType.SPIDER;
            case "rustfang_skeleton" -> org.bukkit.entity.EntityType.SKELETON;
            case "abyss_phantom" -> org.bukkit.entity.EntityType.PHANTOM;
            default -> org.bukkit.entity.EntityType.ZOMBIE;
        };
    }

    private Material rollOre(int y) {
        double r = ThreadLocalRandom.current().nextDouble();
        if (y < -20) {
            if (r < 0.25) return Material.DIAMOND_ORE;
            if (r < 0.40) return Material.EMERALD_ORE;
            if (r < 0.60) return Material.GOLD_ORE;
            if (r < 0.85) return Material.IRON_ORE;
            return Material.REDSTONE_ORE;
        } else {
            if (r < 0.30) return Material.COAL_ORE;
            if (r < 0.80) return Material.IRON_ORE;
            return Material.GOLD_ORE;
        }
    }

    private void populateChestLoot(org.bukkit.inventory.Inventory inv) {
        inv.clear();
        int count = ThreadLocalRandom.current().nextInt(3, 8);
        for (int i = 0; i < count; i++) {
            int slot = ThreadLocalRandom.current().nextInt(inv.getSize());
            inv.setItem(slot, rollLootItem());
        }
    }

    private ItemStack rollLootItem() {
        double customChance = plugin.getConfig().getDouble("deepmine.loot.custom-item-chance", 0.35);
        double vanillaChance = plugin.getConfig().getDouble("deepmine.loot.vanilla-rich-chance", 0.40);
        double roll = ThreadLocalRandom.current().nextDouble();

        try {
            if (roll < customChance) {
                var customItemsSec = plugin.getConfig().getConfigurationSection("deepmine.loot.custom-items");
                if (customItemsSec != null) {
                    int totalWeight = 0;
                    for (String key : customItemsSec.getKeys(false)) {
                        totalWeight += customItemsSec.getInt(key + ".weight", 10);
                    }
                    if (totalWeight > 0) {
                        int r = ThreadLocalRandom.current().nextInt(totalWeight);
                        int currentWeight = 0;
                        for (String key : customItemsSec.getKeys(false)) {
                            currentWeight += customItemsSec.getInt(key + ".weight", 10);
                            if (r < currentWeight) {
                                String id = key;
                                String kind = customItemsSec.getString(key + ".kind", "ITEM");
                                if ("RUNE".equalsIgnoreCase(kind)) {
                                    return runeItemFactory.create(id);
                                } else {
                                    return itemFactory.create(id);
                                }
                            }
                        }
                    }
                }
                return itemFactory.create("reinforce_stone");
            } else if (roll < customChance + vanillaChance) {
                double r = ThreadLocalRandom.current().nextDouble();
                Material choice;
                int amt;
                if (r < 0.12) {
                    Material[] richBlocks = {Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.GOLD_BLOCK, Material.IRON_BLOCK};
                    choice = richBlocks[ThreadLocalRandom.current().nextInt(richBlocks.length)];
                    amt = ThreadLocalRandom.current().nextInt(1, 3);
                } else if (r < 0.42) {
                    Material[] rawBlocks = {Material.RAW_IRON_BLOCK, Material.RAW_GOLD_BLOCK, Material.RAW_COPPER_BLOCK};
                    choice = rawBlocks[ThreadLocalRandom.current().nextInt(rawBlocks.length)];
                    amt = ThreadLocalRandom.current().nextInt(1, 4);
                } else {
                    Material[] minerals = {Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT};
                    choice = minerals[ThreadLocalRandom.current().nextInt(minerals.length)];
                    amt = ThreadLocalRandom.current().nextInt(2, 7);
                }
                return new ItemStack(choice, amt);
            } else {
                int minClam = plugin.getConfig().getInt("deepmine.loot.clam-min", 15);
                int maxClam = plugin.getConfig().getInt("deepmine.loot.clam-max", 60);
                int amount = ThreadLocalRandom.current().nextInt(minClam, maxClam + 1);
                ItemStack clam = new ItemStack(Material.GOLD_NUGGET, amount);
                ItemMeta meta = clam.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6조개");
                    clam.setItemMeta(meta);
                }
                return clam;
            }
        } catch (Exception exception) {
            return new ItemStack(Material.DIAMOND, 1);
        }
    }

    static class SpawnerInfo {
        Location loc;
        String mobId;
        SpawnerInfo(Location loc, String mobId) {
            this.loc = loc;
            this.mobId = mobId;
        }
    }
}
