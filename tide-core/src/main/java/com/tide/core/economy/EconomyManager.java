package com.tide.core.economy;

import com.tide.core.TideCorePlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache backed by an SQLite file or YAML configuration fallback.
 * All cache reads (getClam/getPearl/getRep) are synchronous since the cache is already
 * in memory; all writes are mirrored to disk on an async thread so the main thread never blocks.
 */
public final class EconomyManager implements EconomyAPI, Listener {

    private final TideCorePlugin plugin;
    private final Map<UUID, PlayerEconomy> cache = new ConcurrentHashMap<>();
    private Connection connection;
    private boolean useFileFallback = false;
    private YamlConfiguration yamlConfig;
    private File yamlFile;

    public EconomyManager(TideCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File dbFile = new File(plugin.getDataFolder(), "data/economy.db");
        dbFile.getParentFile().mkdirs();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_economy (" +
                            "uuid TEXT PRIMARY KEY, clam INTEGER NOT NULL DEFAULT 0, " +
                            "pearl INTEGER NOT NULL DEFAULT 0, rep INTEGER NOT NULL DEFAULT 0, " +
                            "hard_mode INTEGER NOT NULL DEFAULT 0)")) {
                statement.executeUpdate();
            }
            plugin.getLogger().info("Successfully initialized SQLite database.");
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to initialize SQLite database: " + exception.getMessage() + ". Falling back to local YAML file storage.");
            useFileFallback = true;
            yamlFile = new File(plugin.getDataFolder(), "data/players.yml");
            if (!yamlFile.exists()) {
                try {
                    yamlFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create players.yml: " + e.getMessage());
                }
            }
            yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
        }
    }

    public void shutdown() {
        for (PlayerEconomy economy : cache.values()) {
            saveSync(economy);
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerEconomy economy = loadOrCreateSync(player.getUniqueId());
            cache.put(player.getUniqueId(), economy);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerEconomy economy = cache.remove(uuid);
        if (economy != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveSync(economy));
        }
    }

    private synchronized PlayerEconomy loadOrCreateSync(UUID uuid) {
        if (useFileFallback) {
            String key = uuid.toString();
            if (yamlConfig.contains(key)) {
                long clam = yamlConfig.getLong(key + ".clam", 0);
                long pearl = yamlConfig.getLong(key + ".pearl", 0);
                int rep = yamlConfig.getInt(key + ".rep", 0);
                boolean hardMode = yamlConfig.getBoolean(key + ".hard_mode", false);
                return new PlayerEconomy(uuid, clam, pearl, rep, hardMode);
            }
            PlayerEconomy fresh = new PlayerEconomy(uuid, 0, 0, 0, false);
            saveSync(fresh);
            return fresh;
        }

        try (PreparedStatement select = connection.prepareStatement(
                "SELECT clam, pearl, rep, hard_mode FROM player_economy WHERE uuid = ?")) {
            select.setString(1, uuid.toString());
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return new PlayerEconomy(uuid, resultSet.getLong("clam"),
                            resultSet.getLong("pearl"), resultSet.getInt("rep"), resultSet.getInt("hard_mode") != 0);
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to load economy profile for " + uuid + ": " + exception.getMessage());
        }

        PlayerEconomy fresh = new PlayerEconomy(uuid, 0, 0, 0, false);
        saveSync(fresh);
        return fresh;
    }

    private synchronized void saveSync(PlayerEconomy economy) {
        if (useFileFallback) {
            String key = economy.getUuid().toString();
            yamlConfig.set(key + ".clam", economy.getClam());
            yamlConfig.set(key + ".pearl", economy.getPearl());
            yamlConfig.set(key + ".rep", economy.getRep());
            yamlConfig.set(key + ".hard_mode", economy.isHardMode());
            try {
                yamlConfig.save(yamlFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save players.yml: " + e.getMessage());
            }
            return;
        }

        try (PreparedStatement upsert = connection.prepareStatement(
                "INSERT INTO player_economy (uuid, clam, pearl, rep, hard_mode) VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET clam = ?, pearl = ?, rep = ?, hard_mode = ?")) {
            upsert.setString(1, economy.getUuid().toString());
            upsert.setLong(2, economy.getClam());
            upsert.setLong(3, economy.getPearl());
            upsert.setInt(4, economy.getRep());
            upsert.setInt(5, economy.isHardMode() ? 1 : 0);
            upsert.setLong(6, economy.getClam());
            upsert.setLong(7, economy.getPearl());
            upsert.setInt(8, economy.getRep());
            upsert.setInt(9, economy.isHardMode() ? 1 : 0);
            upsert.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to save economy profile for " + economy.getUuid() + ": " + exception.getMessage());
        }
    }

    public Map<UUID, PlayerEconomy> getAllPlayers() {
        Map<UUID, PlayerEconomy> all = new HashMap<>();
        if (useFileFallback) {
            for (String key : yamlConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long clam = yamlConfig.getLong(key + ".clam", 0);
                    long pearl = yamlConfig.getLong(key + ".pearl", 0);
                    int rep = yamlConfig.getInt(key + ".rep", 0);
                    boolean hardMode = yamlConfig.getBoolean(key + ".hard_mode", false);
                    all.put(uuid, new PlayerEconomy(uuid, clam, pearl, rep, hardMode));
                } catch (IllegalArgumentException ignored) {}
            }
        } else if (connection != null) {
            try (PreparedStatement select = connection.prepareStatement("SELECT uuid, clam, pearl, rep, hard_mode FROM player_economy");
                 ResultSet resultSet = select.executeQuery()) {
                while (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    all.put(uuid, new PlayerEconomy(uuid, resultSet.getLong("clam"),
                            resultSet.getLong("pearl"), resultSet.getInt("rep"), resultSet.getInt("hard_mode") != 0));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to scan all player profiles: " + exception.getMessage());
            }
        }
        all.putAll(cache);
        return all;
    }

    private PlayerEconomy getOrLoadCached(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadOrCreateSync);
    }

    private void persistAsync(PlayerEconomy economy) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveSync(economy));
    }

    @Override
    public long getClam(UUID uuid) {
        return getOrLoadCached(uuid).getClam();
    }

    @Override
    public void addClam(UUID uuid, long amount) {
        PlayerEconomy economy = getOrLoadCached(uuid);
        economy.setClam(economy.getClam() + amount);
        persistAsync(economy);
    }

    @Override
    public boolean takeClam(UUID uuid, long amount) {
        PlayerEconomy economy = getOrLoadCached(uuid);
        if (economy.getClam() < amount) {
            return false;
        }
        economy.setClam(economy.getClam() - amount);
        persistAsync(economy);
        return true;
    }

    @Override
    public long getPearl(UUID uuid) {
        return getOrLoadCached(uuid).getPearl();
    }

    @Override
    public void addPearl(UUID uuid, long amount) {
        PlayerEconomy economy = getOrLoadCached(uuid);
        economy.setPearl(economy.getPearl() + amount);
        persistAsync(economy);
    }

    @Override
    public boolean takePearl(UUID uuid, long amount) {
        PlayerEconomy economy = getOrLoadCached(uuid);
        if (economy.getPearl() < amount) {
            return false;
        }
        economy.setPearl(economy.getPearl() - amount);
        persistAsync(economy);
        return true;
    }

    @Override
    public void addRep(UUID uuid, int amount) {
        PlayerEconomy economy = getOrLoadCached(uuid);
        economy.setRep(economy.getRep() + amount);
        persistAsync(economy);
    }

    @Override
    public int getRep(UUID uuid) {
        return getOrLoadCached(uuid).getRep();
    }

    @Override
    public RepTier getRepTier(UUID uuid) {
        return RepTier.fromRep(getRep(uuid));
    }

    @Override
    public boolean isHardMode(UUID uuid) {
        return getOrLoadCached(uuid).isHardMode();
    }

    @Override
    public void setHardMode(UUID uuid, boolean hardMode) {
        PlayerEconomy economy = getOrLoadCached(uuid);
        economy.setHardMode(hardMode);
        persistAsync(economy);
    }

    public void updatePlayerEconomy(UUID uuid, Long clam, Long pearl, Integer rep, Boolean hardMode) {
        PlayerEconomy economy = getOrLoadCached(uuid);
        if (clam != null) {
            economy.setClam(clam);
        }
        if (pearl != null) {
            economy.setPearl(pearl);
        }
        if (rep != null) {
            economy.setRep(rep);
        }
        if (hardMode != null) {
            economy.setHardMode(hardMode);
        }
        persistAsync(economy);
    }

    /**
     * Sums clam across cached (online) players only — a lightweight proxy for the
     * inflation monitor. A full server-wide total would require an async DB scan.
     */
    public long getOnlineClamTotal() {
        long total = 0;
        for (PlayerEconomy economy : cache.values()) {
            total += economy.getClam();
        }
        return total;
    }
}
