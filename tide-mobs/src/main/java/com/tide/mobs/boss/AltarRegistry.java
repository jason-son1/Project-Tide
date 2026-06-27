package com.tide.mobs.boss;

import com.tide.core.reload.Reloadable;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public final class AltarRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final File altarsDirectory;
    private final List<SoulAltar> altars = new CopyOnWriteArrayList<>();

    public AltarRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.altarsDirectory = new File(plugin.getDataFolder(), "altars");
    }

    @Override
    public int reload() {
        if (!altarsDirectory.exists()) {
            altarsDirectory.mkdirs();
        }
        List<SoulAltar> loaded = new ArrayList<>();
        File[] files = altarsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        int failures = 0;
        if (files != null) {
            for (File file : files) {
                try {
                    loaded.add(SoulAltar.parse(YamlConfiguration.loadConfiguration(file)));
                } catch (Exception exception) {
                    failures++;
                    plugin.getLogger().log(Level.WARNING,
                            "제단 로드 실패: " + file.getName() + " - " + exception.getMessage(), exception);
                }
            }
        }
        altars.clear();
        altars.addAll(loaded);
        plugin.getLogger().info("보스 제단 로드 완료: 성공 " + loaded.size() + "건, 실패 " + failures + "건");
        return loaded.size();
    }

    public SoulAltar findAt(Location location) {
        for (SoulAltar altar : altars) {
            if (altar.matchesBlock(location)) {
                return altar;
            }
        }
        return null;
    }

    public SoulAltar findById(String id) {
        for (SoulAltar altar : altars) {
            if (altar.getId().equals(id)) {
                return altar;
            }
        }
        return null;
    }

    public java.util.List<SoulAltar> getAll() {
        return java.util.Collections.unmodifiableList(altars);
    }
}

