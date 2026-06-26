package com.tide.rpg.rune;

import com.tide.core.reload.Reloadable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class RuneRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final File runesDirectory;
    private final Map<String, RuneDefinition> definitions = new ConcurrentHashMap<>();

    public RuneRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.runesDirectory = new File(plugin.getDataFolder(), "runes");
    }

    @Override
    public int reload() {
        if (!runesDirectory.exists()) {
            runesDirectory.mkdirs();
        }
        Map<String, RuneDefinition> loaded = new ConcurrentHashMap<>();
        File[] files = runesDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        int failures = 0;
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    RuneDefinition definition = RuneDefinition.parse(yaml);
                    loaded.put(definition.getId(), definition);
                } catch (Exception exception) {
                    failures++;
                    plugin.getLogger().log(Level.WARNING,
                            "룬 로드 실패: " + file.getName() + " - " + exception.getMessage(), exception);
                }
            }
        }
        definitions.clear();
        definitions.putAll(loaded);
        plugin.getLogger().info("룬 로드 완료: 성공 " + loaded.size() + "건, 실패 " + failures + "건");
        return loaded.size();
    }

    public Map<String, RuneDefinition> getAll() {
        return definitions;
    }

    public Optional<RuneDefinition> findByTypeAndGrade(String type, int grade) {
        return definitions.values().stream()
                .filter(d -> d.getType().equalsIgnoreCase(type) && d.getGrade() == grade)
                .findFirst();
    }

    public Optional<RuneDefinition> findFusionRecipeFor(String inputId) {
        return definitions.values().stream()
                .filter(d -> inputId.equals(d.getFusionInputId()))
                .findFirst();
    }
}
