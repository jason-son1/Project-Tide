package com.tide.mobs.boss;

import com.tide.core.reload.Reloadable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

        ensureArenasBuilt(loaded);
        return loaded.size();
    }

    /**
     * Altars registered straight from a bundled YAML file never went through
     * /altar create, so AltarBuilder/BossArenaBuilder were never called for
     * them — visiting one showed only bare terrain. Build (or re-verify) the
     * arena for every loaded altar so this holds regardless of how it was
     * registered.
     */
    private void ensureArenasBuilt(List<SoulAltar> loaded) {
        for (SoulAltar altar : loaded) {
            World world = Bukkit.getWorld(altar.getWorld());
            if (world == null) {
                continue;
            }
            Location center = new Location(world, altar.getBlockX(), altar.getBlockY(), altar.getBlockZ());
            BossArenaBuilder.build(center, altar.getBossType());
            AltarBuilder.build(center);
        }
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

