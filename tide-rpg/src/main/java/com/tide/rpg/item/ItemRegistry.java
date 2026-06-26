package com.tide.rpg.item;

import com.tide.core.reload.Reloadable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Loads every items/*.yml on enable and on `/tide reload items`. */
public final class ItemRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final File itemsDirectory;
    private final Map<String, ItemDefinition> definitions = new ConcurrentHashMap<>();

    public ItemRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemsDirectory = new File(plugin.getDataFolder(), "items");
    }

    @Override
    public int reload() {
        if (!itemsDirectory.exists()) {
            itemsDirectory.mkdirs();
        }
        Map<String, ItemDefinition> loaded = new ConcurrentHashMap<>();
        File[] files = itemsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        int failures = 0;
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    ItemDefinition definition = ItemDefinition.parse(yaml);
                    loaded.put(definition.getId(), definition);
                } catch (Exception exception) {
                    failures++;
                    plugin.getLogger().log(Level.WARNING,
                            "아이템 로드 실패: " + file.getName() + " - " + exception.getMessage(), exception);
                }
            }
        }
        definitions.clear();
        definitions.putAll(loaded);
        plugin.getLogger().info("아이템 로드 완료: 성공 " + loaded.size() + "건, 실패 " + failures + "건");
        return loaded.size();
    }

    public ItemDefinition get(String id) {
        return definitions.get(id);
    }

    public Map<String, ItemDefinition> getAll() {
        return definitions;
    }
}
