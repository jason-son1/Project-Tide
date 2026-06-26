package com.tide.rpg.fishing;

import com.tide.core.reload.Reloadable;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public final class FishingHoleRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final File directory;
    private final List<FishingHole> holes = new CopyOnWriteArrayList<>();

    public FishingHoleRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "fishingholes");
    }

    @Override
    public int reload() {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        List<FishingHole> loaded = new ArrayList<>();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        int failures = 0;
        if (files != null) {
            for (File file : files) {
                try {
                    loaded.add(FishingHole.parse(YamlConfiguration.loadConfiguration(file)));
                } catch (Exception exception) {
                    failures++;
                    plugin.getLogger().log(Level.WARNING,
                            "낚시터 로드 실패: " + file.getName() + " - " + exception.getMessage(), exception);
                }
            }
        }
        holes.clear();
        holes.addAll(loaded);
        plugin.getLogger().info("낚시터 로드 완료: 성공 " + loaded.size() + "건, 실패 " + failures + "건");
        return loaded.size();
    }

    public boolean isInsideAny(Location location) {
        for (FishingHole hole : holes) {
            if (hole.contains(location)) {
                return true;
            }
        }
        return false;
    }
}
