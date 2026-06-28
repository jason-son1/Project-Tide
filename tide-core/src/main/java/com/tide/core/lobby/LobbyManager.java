package com.tide.core.lobby;

import com.tide.core.TideCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class LobbyManager {

    private final TideCorePlugin plugin;
    private final File dataFile;
    private final NamespacedKey npcKey;
    
    private Location lobbySpawn;
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();
    private final Map<UUID, Location> pendingLocations = new HashMap<>();

    public LobbyManager(TideCorePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/lobby.yml");
        this.npcKey = new NamespacedKey(plugin, "lobby_npc");
        loadLobbyData();
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public NamespacedKey getNpcKey() {
        return npcKey;
    }

    /**
     * Loads lobby configuration data from data/lobby.yml
     */
    public void loadLobbyData() {
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String worldName = config.getString("lobby.world");
        if (worldName == null) return;

        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world != null) {
            double x = config.getDouble("lobby.x");
            double y = config.getDouble("lobby.y");
            double z = config.getDouble("lobby.z");
            float yaw = (float) config.getDouble("lobby.yaw", 180.0);
            float pitch = (float) config.getDouble("lobby.pitch", 0.0);
            this.lobbySpawn = new Location(world, x, y, z, yaw, pitch);
        }
    }

    /**
     * Saves lobby configuration data to data/lobby.yml
     */
    public void saveLobbyData() {
        if (lobbySpawn == null) return;
        YamlConfiguration config = new YamlConfiguration();
        config.set("lobby.world", lobbySpawn.getWorld().getName());
        config.set("lobby.x", lobbySpawn.getX());
        config.set("lobby.y", lobbySpawn.getY());
        config.set("lobby.z", lobbySpawn.getZ());
        config.set("lobby.yaw", lobbySpawn.getYaw());
        config.set("lobby.pitch", lobbySpawn.getPitch());
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save lobby data: " + e.getMessage());
        }
    }

    /**
     * Teleports a player to the lobby with survival-mode delay check
     */
    public void teleportToLobby(Player player) {
        if (lobbySpawn == null) {
            player.sendMessage("§c로비가 아직 생성되지 않았습니다. 관리자에게 문의하세요.");
            return;
        }

        UUID uuid = player.getUniqueId();
        cancelTeleport(player, false); // Cancel any active teleportation first

        // Immediate teleport for non-survival modes
        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.teleport(lobbySpawn);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.sendMessage("§a로비로 이동했습니다.");
            return;
        }

        // Survival mode 5 seconds delay
        player.sendMessage("§e5초 후 로비로 이동합니다. 움직이거나 피해를 입으면 취소됩니다.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 0.8f);
        pendingLocations.put(uuid, player.getLocation().clone());

        BukkitTask task = new BukkitRunnable() {
            int secondsLeft = 5;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    cleanup(uuid);
                    return;
                }

                // Check movement
                Location startLoc = pendingLocations.get(uuid);
                Location currentLoc = player.getLocation();
                if (startLoc == null || startLoc.getWorld() != currentLoc.getWorld() ||
                        startLoc.getBlockX() != currentLoc.getBlockX() ||
                        startLoc.getBlockY() != currentLoc.getBlockY() ||
                        startLoc.getBlockZ() != currentLoc.getBlockZ()) {
                    cancel();
                    player.sendMessage("§c움직임이 감지되어 이동이 취소되었습니다.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.8f);
                    cleanup(uuid);
                    return;
                }

                secondsLeft--;
                if (secondsLeft <= 0) {
                    cancel();
                    player.teleport(lobbySpawn);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    player.sendMessage("§b로비에 도착했습니다!");
                    cleanup(uuid);
                } else {
                    player.sendMessage("§b로비 이동까지 §e" + secondsLeft + "초 §b남았습니다...");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        pendingTeleports.put(uuid, task);
    }

    /**
     * Cancels a pending teleport for a player
     */
    public void cancelTeleport(Player player, boolean notify) {
        UUID uuid = player.getUniqueId();
        if (pendingTeleports.containsKey(uuid)) {
            pendingTeleports.get(uuid).cancel();
            if (notify) {
                player.sendMessage("§c피해를 입거나 행동에 간섭이 생겨 로비 이동이 취소되었습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.8f);
            }
            cleanup(uuid);
        }
    }

    private void cleanup(UUID uuid) {
        pendingTeleports.remove(uuid);
        pendingLocations.remove(uuid);
    }

    public void cancelAllTeleports() {
        for (BukkitTask task : pendingTeleports.values()) {
            task.cancel();
        }
        pendingTeleports.clear();
        pendingLocations.clear();
    }

    /**
     * Creates a new lobby at a random sky location
     */
    public void createLobby(World world) {
        // 1. Pick a random coordinates in world
        Random random = new Random();
        int rx = random.nextInt(10000) - 5000;
        int rz = random.nextInt(10000) - 5000;
        int ry = 250;

        Location center = new Location(world, rx, ry, rz);

        // 2. Clear old lobby if it exists
        clearOldLobby();

        // 3. Build the new lobby
        buildLobbyStructure(center);

        // 4. Set lobbySpawn
        this.lobbySpawn = center.clone().add(0.5, 1.0, 0.5);
        this.lobbySpawn.setYaw(180.0f); // Face Z+ (NPC will spawn in Z+)
        this.lobbySpawn.setPitch(0.0f);

        // 5. Spawn Lobby NPC
        spawnLobbyNPC(center);

        // 6. Save lobby data
        saveLobbyData();
    }

    /**
     * Recreates the lobby by clearing the old one and creating a new one
     */
    public void recreateLobby(World world) {
        createLobby(world);
    }

    /**
     * Checks if a location is inside the lobby region
     */
    public boolean isInsideLobby(Location loc) {
        if (lobbySpawn == null || loc == null) return false;
        if (!loc.getWorld().getName().equals(lobbySpawn.getWorld().getName())) return false;

        double cx = lobbySpawn.getX();
        double cy = lobbySpawn.getY();
        double cz = lobbySpawn.getZ();

        // Check bounding box around the lobby spawn: X: ±25, Y: -5 to +15, Z: ±25
        return loc.getX() >= cx - 25 && loc.getX() <= cx + 25 &&
               loc.getY() >= cy - 5  && loc.getY() <= cy + 15 &&
               loc.getZ() >= cz - 25 && loc.getZ() <= cz + 25;
    }

    /**
     * Clears blocks of the previously saved lobby
     */
    private void clearOldLobby() {
        if (lobbySpawn == null) return;
        World world = lobbySpawn.getWorld();
        int cx = lobbySpawn.getBlockX();
        int cy = lobbySpawn.getBlockY();
        int cz = lobbySpawn.getBlockZ();

        // Remove old NPC entities in this chunk/area
        removeLobbyEntities(world, cx, cy, cz);

        // Clear blocks in a 27x27x20 area (from cy-2 to cy+15)
        for (int x = cx - 13; x <= cx + 13; x++) {
            for (int z = cz - 13; z <= cz + 13; z++) {
                for (int y = cy - 2; y <= cy + 15; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
    }

    /**
     * Scans and removes any entities with the custom PDC tag in the world
     */
    private void removeLobbyEntities(World world, int cx, int cy, int cz) {
        // Remove globally in the world to prevent leakage
        for (Entity entity : world.getEntities()) {
            if (entity.getPersistentDataContainer().has(npcKey, PersistentDataType.BYTE)) {
                entity.remove();
            }
        }
    }

    /**
     * Builds the lobby structure block by block
     */
    private void buildLobbyStructure(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 1. Build floor (Y = cy) and under-floor support (Y = cy - 1)
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                int bx = cx + x;
                int bz = cz + z;

                // Under floor support block
                world.getBlockAt(bx, cy - 1, bz).setType(Material.QUARTZ_BLOCK);

                // Center 9x9 Glass floor with Sea Lantern underneath
                if (Math.abs(x) <= 4 && Math.abs(z) <= 4) {
                    world.getBlockAt(bx, cy, bz).setType(Material.BLUE_STAINED_GLASS);
                    world.getBlockAt(bx, cy - 1, bz).setType(Material.SEA_LANTERN);
                } 
                // Border pattern
                else if (Math.abs(x) == 12 || Math.abs(z) == 12) {
                    world.getBlockAt(bx, cy, bz).setType(Material.PRISMARINE_BRICKS);
                } 
                // Diagonal stripes
                else if (Math.abs(x) == Math.abs(z)) {
                    world.getBlockAt(bx, cy, bz).setType(Material.PRISMARINE_BRICKS);
                }
                // Inner floor
                else {
                    world.getBlockAt(bx, cy, bz).setType(Material.QUARTZ_BLOCK);
                }
            }
        }

        // 2. Build Walls (Y = cy + 1 to cy + 6)
        for (int y = 1; y <= 6; y++) {
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 12; z++) {
                    int bx = cx + x;
                    int by = cy + y;
                    int bz = cz + z;

                    boolean isCorner = (Math.abs(x) == 12 && Math.abs(z) == 12);
                    boolean isWall = (Math.abs(x) == 12 || Math.abs(z) == 12);

                    if (isCorner) {
                        world.getBlockAt(bx, by, bz).setType(Material.QUARTZ_PILLAR);
                    } else if (isWall) {
                        // Double-layer back walls for windows to prevent viewing outside
                        // Window on East wall (x == 12)
                        if (x == 12 && (y == 3 || y == 4) && Math.abs(z) <= 2) {
                            world.getBlockAt(bx, by, bz).setType(Material.BLUE_STAINED_GLASS);
                            world.getBlockAt(bx + 1, by, bz).setType(Material.QUARTZ_BLOCK); // Backing
                        }
                        // Window on West wall (x == -12)
                        else if (x == -12 && (y == 3 || y == 4) && Math.abs(z) <= 2) {
                            world.getBlockAt(bx, by, bz).setType(Material.BLUE_STAINED_GLASS);
                            world.getBlockAt(bx - 1, by, bz).setType(Material.QUARTZ_BLOCK); // Backing
                        }
                        // Window on South wall (z == 12)
                        else if (z == 12 && (y == 3 || y == 4) && Math.abs(x) <= 2) {
                            world.getBlockAt(bx, by, bz).setType(Material.BLUE_STAINED_GLASS);
                            world.getBlockAt(bx, by, bz + 1).setType(Material.QUARTZ_BLOCK); // Backing
                        }
                        // Normal wall patterns
                        else {
                            if (y == 3 && (x % 3 == 0 || z % 3 == 0)) {
                                world.getBlockAt(bx, by, bz).setType(Material.SEA_LANTERN);
                            } else if ((y == 3 || y == 4) && (Math.abs(x) == 6 || Math.abs(z) == 6)) {
                                world.getBlockAt(bx, by, bz).setType(Material.CHISELED_QUARTZ_BLOCK);
                            } else {
                                world.getBlockAt(bx, by, bz).setType(Material.QUARTZ_BLOCK);
                            }
                        }
                    }
                }
            }
        }

        // 3. Build Ceiling (Y = cy + 7)
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                int bx = cx + x;
                int bz = cz + z;

                if (Math.abs(x) <= 4 && Math.abs(z) <= 4) {
                    world.getBlockAt(bx, cy + 7, bz).setType(Material.BLUE_STAINED_GLASS);
                } else {
                    world.getBlockAt(bx, cy + 7, bz).setType(Material.QUARTZ_BLOCK);
                }
            }
        }

        // 4. Create premium Water Fountain/Pillars in the 4 corners (offset 10)
        int[] corners = {-10, 10};
        for (int cxOffset : corners) {
            for (int czOffset : corners) {
                int wX = cx + cxOffset;
                int wZ = cz + czOffset;

                // Basin at Y = cy
                world.getBlockAt(wX, cy, wZ).setType(Material.PRISMARINE_BRICKS);

                // Place Stairs around the basin block to catch water
                setStairBasin(world, wX, cy, wZ);

                // Water source at Y = cy + 6 flowing down
                world.getBlockAt(wX, cy + 6, wZ).setType(Material.WATER);
            }
        }

        // 5. Add exit button and sign on the North wall (Z = -12, X = 0)
        setupExitButtonAndSign(world, cx, cy, cz);
    }

    private void setupExitButtonAndSign(World world, int cx, int cy, int cz) {
        // Ensure wall backing exists
        world.getBlockAt(cx, cy + 2, cz - 12).setType(Material.QUARTZ_BLOCK);
        world.getBlockAt(cx, cy + 3, cz - 12).setType(Material.QUARTZ_BLOCK);

        // Stone Button on the wall facing South (placed at Z = -11)
        org.bukkit.block.Block buttonBlock = world.getBlockAt(cx, cy + 2, cz - 11);
        buttonBlock.setType(Material.STONE_BUTTON);
        if (buttonBlock.getBlockData() instanceof org.bukkit.block.data.Directional directional) {
            directional.setFacing(BlockFace.SOUTH);
            buttonBlock.setBlockData(directional);
        }

        // Wall Sign on the wall facing South (placed at Z = -11)
        org.bukkit.block.Block signBlock = world.getBlockAt(cx, cy + 3, cz - 11);
        signBlock.setType(Material.OAK_WALL_SIGN);
        if (signBlock.getBlockData() instanceof org.bukkit.block.data.Directional directional) {
            directional.setFacing(BlockFace.SOUTH);
            signBlock.setBlockData(directional);
        }

        // Write Sign text
        org.bukkit.block.BlockState state = signBlock.getState();
        if (state instanceof org.bukkit.block.Sign sign) {
            sign.setLine(0, "§9§l[스폰지점으로]");
            sign.setLine(1, "§0우클릭 또는");
            sign.setLine(2, "§0아래 버튼 클릭");
            sign.setLine(3, "§0또는 유리 통과");
            sign.update();
        }

        // Escape portal floor blocks
        for (int dx = -1; dx <= 1; dx++) {
            world.getBlockAt(cx + dx, cy, cz - 11).setType(Material.LIGHT_BLUE_STAINED_GLASS);
            world.getBlockAt(cx + dx, cy - 1, cz - 11).setType(Material.SEA_LANTERN);
        }
    }

    private void setStairBasin(World world, int x, int y, int z) {
        // North stair (facing South)
        setStair(world, x, y, z - 1, BlockFace.SOUTH);
        // South stair (facing North)
        setStair(world, x, y, z + 1, BlockFace.NORTH);
        // West stair (facing East)
        setStair(world, x - 1, y, z, BlockFace.EAST);
        // East stair (facing West)
        setStair(world, x + 1, y, z, BlockFace.WEST);
    }

    private void setStair(World world, int x, int y, int z, BlockFace face) {
        org.bukkit.block.Block block = world.getBlockAt(x, y, z);
        block.setType(Material.QUARTZ_STAIRS);
        if (block.getBlockData() instanceof Stairs stairs) {
            stairs.setFacing(face);
            block.setBlockData(stairs);
        }
    }

    /**
     * Spawns the spawn teleport NPC inside the lobby
     */
    private void spawnLobbyNPC(Location center) {
        World world = center.getWorld();
        // Spawn NPC 6 blocks in front of the center spawn point (towards Z+)
        Location npcLoc = center.clone().add(0.5, 1.0, 6.5);
        npcLoc.setYaw(0.0f); // Facing Z- (towards the player arrival point)
        npcLoc.setPitch(0.0f);

        Villager villager = (Villager) world.spawnEntity(npcLoc, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setRemoveWhenFarAway(false);
        villager.setCustomName("§b§l스폰지점으로 이동 §7[클릭]");
        villager.setCustomNameVisible(true);

        // Store custom PDC key to identify this NPC uniquely
        villager.getPersistentDataContainer().set(npcKey, PersistentDataType.BYTE, (byte) 1);
    }
}

