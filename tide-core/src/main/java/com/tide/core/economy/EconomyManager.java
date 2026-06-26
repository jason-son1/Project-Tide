package com.tide.core.economy;

import com.tide.core.TideCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache backed by an SQLite file. All cache reads (getClam/getPearl/getRep)
 * are synchronous since the cache is already in memory; all writes are mirrored to
 * SQLite on an async thread so the main thread never blocks on disk I/O.
 */
public final class EconomyManager implements EconomyAPI, Listener {

    private final TideCorePlugin plugin;
    private final Map<UUID, PlayerEconomy> cache = new ConcurrentHashMap<>();
    private Connection connection;

    public EconomyManager(TideCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File dbFile = new File(plugin.getDataFolder(), "data/economy.db");
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
        } catch (ClassNotFoundException | SQLException exception) {
            plugin.getLogger().severe("Failed to initialize economy database: " + exception.getMessage());
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
