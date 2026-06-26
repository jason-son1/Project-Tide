package com.tide.rpg.tideext;

import com.tide.core.reload.Reloadable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public final class GateRegistry implements Reloadable {

    private final JavaPlugin plugin;
    private final File gatesDirectory;
    private final List<GateDefinition> gates = new CopyOnWriteArrayList<>();

    public GateRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gatesDirectory = new File(plugin.getDataFolder(), "gates");
    }

    @Override
    public int reload() {
        if (!gatesDirectory.exists()) {
            gatesDirectory.mkdirs();
        }
        List<GateDefinition> loaded = new ArrayList<>();
        File[] files = gatesDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        int failures = 0;
        if (files != null) {
            for (File file : files) {
                try {
                    loaded.add(GateDefinition.parse(YamlConfiguration.loadConfiguration(file)));
                } catch (Exception exception) {
                    failures++;
                    plugin.getLogger().log(Level.WARNING,
                            "조수 게이트 로드 실패: " + file.getName() + " - " + exception.getMessage(), exception);
                }
            }
        }
        gates.clear();
        gates.addAll(loaded);
        plugin.getLogger().info("조수 게이트 로드 완료: 성공 " + loaded.size() + "건, 실패 " + failures + "건");
        return loaded.size();
    }

    public List<GateDefinition> all() {
        return gates;
    }
}
