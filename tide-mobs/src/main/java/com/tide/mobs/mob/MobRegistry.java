package com.tide.mobs.mob;

import com.tide.core.reload.Reloadable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class MobRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final File mobsDirectory;
    private final Map<String, CustomMob> mobs = new ConcurrentHashMap<>();

    public MobRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mobsDirectory = new File(plugin.getDataFolder(), "mobs");
    }

    @Override
    public int reload() {
        if (!mobsDirectory.exists()) {
            mobsDirectory.mkdirs();
        }
        Map<String, CustomMob> loaded = new ConcurrentHashMap<>();
        File[] files = mobsDirectory.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        int failures = 0;
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    
                    String id = yaml.getString("id");
                    if (id == null || id.isBlank()) {
                        id = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    }
                    String displayName = yaml.getString("display_name", id);
                    String baseMob = yaml.getString("base_mob", "ZOMBIE");
                    int customModelData = yaml.getInt("custom_model_data", 0);
                    
                    double hpMultiplier = yaml.getDouble("stats.hp_multiplier", 1.0);
                    double damageMultiplier = yaml.getDouble("stats.damage_multiplier", 1.0);
                    double movementSpeed = yaml.getDouble("stats.movement_speed", -1.0);
                    
                    List<String> affixes = yaml.getStringList("affixes");
                    List<String> spawnWorlds = yaml.getStringList("spawn.worlds");
                    List<String> spawnBiomes = yaml.getStringList("spawn.biomes");
                    List<String> spawnTideStates = yaml.getStringList("spawn.tide_states");
                    int spawnWeight = yaml.getInt("spawn.weight", 10);
                    
                    List<Map<String, Object>> drops = new ArrayList<>();
                    List<Map<?, ?>> rawDrops = yaml.getMapList("drops");
                    if (rawDrops != null) {
                        for (Map<?, ?> rawMap : rawDrops) {
                            Map<String, Object> map = new HashMap<>();
                            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                map.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                            drops.add(map);
                        }
                    }
                    
                    CustomMob customMob = new CustomMob(id, displayName, baseMob, customModelData,
                            hpMultiplier, damageMultiplier, movementSpeed,
                            affixes, spawnWorlds, spawnBiomes, spawnTideStates, spawnWeight, drops);
                            
                    loaded.put(customMob.getId(), customMob);
                } catch (Exception exception) {
                    failures++;
                    plugin.getLogger().log(Level.WARNING,
                            "커스텀 몹 로드 실패: " + file.getName() + " - " + exception.getMessage(), exception);
                }
            }
        }
        mobs.clear();
        mobs.putAll(loaded);
        plugin.getLogger().info("커스텀 몹 로드 완료: 성공 " + loaded.size() + "건, 실패 " + failures + "건");
        return loaded.size();
    }

    public CustomMob get(String id) {
        return mobs.get(id);
    }

    public List<CustomMob> all() {
        return new ArrayList<>(mobs.values());
    }
}
