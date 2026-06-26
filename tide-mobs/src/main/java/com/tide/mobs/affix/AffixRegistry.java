package com.tide.mobs.affix;

import com.tide.core.reload.Reloadable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class AffixRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final File affixesDirectory;
    private final Map<String, AffixDefinition> definitions = new ConcurrentHashMap<>();

    public AffixRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.affixesDirectory = new File(plugin.getDataFolder(), "affixes");
    }

    @Override
    public int reload() {
        if (!affixesDirectory.exists()) {
            affixesDirectory.mkdirs();
        }
        Map<String, AffixDefinition> loaded = new ConcurrentHashMap<>();
        File[] files = affixesDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        int failures = 0;
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    AffixDefinition definition = AffixDefinition.parse(yaml);
                    loaded.put(definition.getId(), definition);
                } catch (Exception exception) {
                    failures++;
                    plugin.getLogger().log(Level.WARNING,
                            "접두사 로드 실패: " + file.getName() + " - " + exception.getMessage(), exception);
                }
            }
        }
        definitions.clear();
        definitions.putAll(loaded);
        plugin.getLogger().info("접두사 로드 완료: 성공 " + loaded.size() + "건, 실패 " + failures + "건");
        return loaded.size();
    }

    public AffixDefinition get(String id) {
        return definitions.get(id);
    }

    public List<AffixDefinition> all() {
        return new ArrayList<>(definitions.values());
    }
}
