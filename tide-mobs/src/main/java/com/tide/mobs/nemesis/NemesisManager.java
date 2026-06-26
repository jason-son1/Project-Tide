package com.tide.mobs.nemesis;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists across restarts (SQLite or YAML fallback configuration) so a nemesis
 * that's still alive keeps hunting its target after a server reboot.
 */
public final class NemesisManager {

    private final JavaPlugin plugin;
    private final Map<UUID, NemesisRecord> activeByPlayer = new ConcurrentHashMap<>();
    private Connection connection;
    private boolean useFileFallback = false;
    private YamlConfiguration yamlConfig;
    private File yamlFile;

    public NemesisManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File dbFile = new File(plugin.getDataFolder(), "data/nemesis.db");
        dbFile.getParentFile().mkdirs();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS nemesis (" +
                            "mob_uuid TEXT PRIMARY KEY, player_uuid TEXT NOT NULL, original_name TEXT, " +
                            "affixes TEXT, kill_count INTEGER NOT NULL DEFAULT 1, is_active INTEGER NOT NULL DEFAULT 1)")) {
                statement.executeUpdate();
            }
            loadActiveRecords();
            plugin.getLogger().info("Successfully initialized SQLite nemesis database.");
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to initialize SQLite nemesis database: " + exception.getMessage() + ". Falling back to local YAML file storage.");
            useFileFallback = true;
            yamlFile = new File(plugin.getDataFolder(), "data/nemesis.yml");
            if (!yamlFile.exists()) {
                try {
                    yamlFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create nemesis.yml: " + e.getMessage());
                }
            }
            yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
            loadActiveRecords();
        }
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private void loadActiveRecords() {
        if (useFileFallback) {
            for (String keyStr : yamlConfig.getKeys(false)) {
                boolean isActive = yamlConfig.getBoolean(keyStr + ".is_active", false);
                if (isActive) {
                    NemesisRecord record = new NemesisRecord(
                            UUID.fromString(keyStr),
                            UUID.fromString(yamlConfig.getString(keyStr + ".player_uuid")),
                            yamlConfig.getString(keyStr + ".original_name"),
                            yamlConfig.getString(keyStr + ".affixes"),
                            yamlConfig.getInt(keyStr + ".kill_count"),
                            true);
                    activeByPlayer.put(record.getPlayerUuid(), record);
                }
            }
            return;
        }

        try (PreparedStatement select = connection.prepareStatement(
                "SELECT mob_uuid, player_uuid, original_name, affixes, kill_count FROM nemesis WHERE is_active = 1");
             ResultSet resultSet = select.executeQuery()) {
            while (resultSet.next()) {
                NemesisRecord record = new NemesisRecord(
                        UUID.fromString(resultSet.getString("mob_uuid")),
                        UUID.fromString(resultSet.getString("player_uuid")),
                        resultSet.getString("original_name"),
                        resultSet.getString("affixes"),
                        resultSet.getInt("kill_count"),
                        true);
                activeByPlayer.put(record.getPlayerUuid(), record);
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to load nemesis records: " + exception.getMessage());
        }
    }

    public NemesisRecord getActiveFor(UUID playerUuid) {
        return activeByPlayer.get(playerUuid);
    }

    public java.util.Collection<NemesisRecord> getAllActive() {
        return activeByPlayer.values();
    }

    public NemesisRecord findByMob(UUID mobUuid) {
        for (NemesisRecord record : activeByPlayer.values()) {
            if (record.getMobUuid().equals(mobUuid)) {
                return record;
            }
        }
        return null;
    }

    /** Awakens (or re-strikes) a nemesis. Returns the record, or null if the player already has one active. */
    public NemesisRecord awaken(LivingEntity mob, Player victim, String affixesCsv) {
        NemesisRecord existing = activeByPlayer.get(victim.getUniqueId());
        if (existing != null) {
            if (!existing.getMobUuid().equals(mob.getUniqueId())) {
                return null; // player already has a different active nemesis
            }
            existing.incrementKillCount();
            persistAsync(existing);
            applyAwakenEffects(mob, victim, existing);
            return existing;
        }

        String originalName = mob.getType().name().toLowerCase().replace('_', ' ');
        NemesisRecord record = new NemesisRecord(mob.getUniqueId(), victim.getUniqueId(), originalName, affixesCsv, 1, true);
        activeByPlayer.put(victim.getUniqueId(), record);
        persistAsync(record);
        applyAwakenEffects(mob, victim, record);
        return record;
    }

    private void applyAwakenEffects(LivingEntity mob, Player victim, NemesisRecord record) {
        String affixLabel = record.getAffixesCsv() == null || record.getAffixesCsv().isBlank()
                ? "" : "[" + record.getAffixesCsv() + "] ";
        mob.setCustomName("§4[네메시스] §c" + affixLabel + "§f" + record.getOriginalName()
                + " §8<" + victim.getName() + "의 복수자>");
        mob.setCustomNameVisible(true);
        mob.setGlowing(true);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
        mob.getWorld().spawnParticle(org.bukkit.Particle.FLAME, mob.getLocation(), 40, 1, 1, 1);
        mob.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE, mob.getLocation(), 40, 1, 1, 1);
        victim.sendTitle("§4§l[네메시스 각성]", "§c" + record.getOriginalName() + "이(가) 당신을 노리고 있습니다!", 10, 70, 20);
    }

    public void deactivate(NemesisRecord record) {
        record.setActive(false);
        activeByPlayer.remove(record.getPlayerUuid());
        persistAsync(record);
    }

    private void persistAsync(NemesisRecord record) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> persistSync(record));
    }

    private synchronized void persistSync(NemesisRecord record) {
        if (useFileFallback) {
            String key = record.getMobUuid().toString();
            yamlConfig.set(key + ".player_uuid", record.getPlayerUuid().toString());
            yamlConfig.set(key + ".original_name", record.getOriginalName());
            yamlConfig.set(key + ".affixes", record.getAffixesCsv());
            yamlConfig.set(key + ".kill_count", record.getKillCount());
            yamlConfig.set(key + ".is_active", record.isActive());
            try {
                yamlConfig.save(yamlFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save nemesis.yml: " + e.getMessage());
            }
            return;
        }

        try (PreparedStatement upsert = connection.prepareStatement(
                "INSERT INTO nemesis (mob_uuid, player_uuid, original_name, affixes, kill_count, is_active) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(mob_uuid) DO UPDATE SET kill_count = ?, is_active = ?")) {
            upsert.setString(1, record.getMobUuid().toString());
            upsert.setString(2, record.getPlayerUuid().toString());
            upsert.setString(3, record.getOriginalName());
            upsert.setString(4, record.getAffixesCsv());
            upsert.setInt(5, record.getKillCount());
            upsert.setInt(6, record.isActive() ? 1 : 0);
            upsert.setInt(7, record.getKillCount());
            upsert.setInt(8, record.isActive() ? 1 : 0);
            upsert.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to save nemesis record: " + exception.getMessage());
        }
    }

    public java.util.Collection<NemesisRecord> getAllRecords() {
        java.util.List<NemesisRecord> all = new java.util.ArrayList<>();
        if (useFileFallback) {
            for (String keyStr : yamlConfig.getKeys(false)) {
                try {
                    NemesisRecord record = new NemesisRecord(
                            UUID.fromString(keyStr),
                            UUID.fromString(yamlConfig.getString(keyStr + ".player_uuid")),
                            yamlConfig.getString(keyStr + ".original_name"),
                            yamlConfig.getString(keyStr + ".affixes"),
                            yamlConfig.getInt(keyStr + ".kill_count"),
                            yamlConfig.getBoolean(keyStr + ".is_active", false)
                    );
                    all.add(record);
                } catch (Exception ignored) {}
            }
        } else if (connection != null) {
            try (PreparedStatement select = connection.prepareStatement("SELECT mob_uuid, player_uuid, original_name, affixes, kill_count, is_active FROM nemesis");
                 ResultSet resultSet = select.executeQuery()) {
                while (resultSet.next()) {
                    all.add(new NemesisRecord(
                            UUID.fromString(resultSet.getString("mob_uuid")),
                            UUID.fromString(resultSet.getString("player_uuid")),
                            resultSet.getString("original_name"),
                            resultSet.getString("affixes"),
                            resultSet.getInt("kill_count"),
                            resultSet.getInt("is_active") != 0));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to query nemesis database: " + exception.getMessage());
            }
        }
        return all;
    }
}
