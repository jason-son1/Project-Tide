package com.tide.core.guide;

import com.tide.core.reload.Reloadable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * YAML-backed registry for in-game tutorial/guide entries, following the
 * same load-from-data-folder pattern as AffixRegistry/AltarRegistry so the
 * content can be hot-reloaded via /tide reload guide without a restart.
 */
public final class GuideRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final File guideDirectory;
    private final Map<String, GuideEntry> entries = new LinkedHashMap<>();

    public GuideRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.guideDirectory = new File(plugin.getDataFolder(), "guide");
    }

    @Override
    public int reload() {
        if (!guideDirectory.exists()) {
            guideDirectory.mkdirs();
        }
        Map<String, GuideEntry> loaded = new LinkedHashMap<>();
        File[] files = guideDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        int failures = 0;
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    GuideEntry entry = GuideEntry.parse(yaml);
                    loaded.put(entry.getId(), entry);
                } catch (Exception exception) {
                    failures++;
                    plugin.getLogger().log(Level.WARNING,
                            "가이드 항목 로드 실패: " + file.getName() + " - " + exception.getMessage(), exception);
                }
            }
        }
        entries.clear();
        entries.putAll(loaded);
        plugin.getLogger().info("가이드 항목 로드 완료: 성공 " + loaded.size() + "건, 실패 " + failures + "건");
        return loaded.size();
    }

    public List<GuideEntry> byCategory(GuideCategory category) {
        List<GuideEntry> result = new ArrayList<>();
        for (GuideEntry entry : entries.values()) {
            if (entry.getCategory() == category) {
                result.add(entry);
            }
        }
        result.sort(Comparator.comparingInt(GuideEntry::getOrder));
        return result;
    }

    public int countByCategory(GuideCategory category) {
        int count = 0;
        for (GuideEntry entry : entries.values()) {
            if (entry.getCategory() == category) {
                count++;
            }
        }
        return count;
    }

    public GuideEntry get(String id) {
        return entries.get(id);
    }
}
