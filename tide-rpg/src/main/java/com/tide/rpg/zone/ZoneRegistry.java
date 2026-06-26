package com.tide.rpg.zone;

import com.tide.core.reload.Reloadable;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public final class ZoneRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final File zonesDirectory;
    private final List<ZoneDefinition> zones = new CopyOnWriteArrayList<>();

    public ZoneRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.zonesDirectory = new File(plugin.getDataFolder(), "zones");
    }

    @Override
    public int reload() {
        if (!zonesDirectory.exists()) {
            zonesDirectory.mkdirs();
        }
        List<ZoneDefinition> loaded = new ArrayList<>();
        File[] files = zonesDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        int failures = 0;
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    loaded.add(ZoneDefinition.parse(yaml));
                } catch (Exception exception) {
                    failures++;
                    plugin.getLogger().log(Level.WARNING,
                            "구역 로드 실패: " + file.getName() + " - " + exception.getMessage(), exception);
                }
            }
        }
        zones.clear();
        zones.addAll(loaded);
        plugin.getLogger().info("구역 로드 완료: 성공 " + loaded.size() + "건, 실패 " + failures + "건");
        return loaded.size();
    }

    public ZoneDefinition findZoneAt(Location location) {
        for (ZoneDefinition zone : zones) {
            if (zone.contains(location)) {
                return zone;
            }
        }
        return null;
    }
}
