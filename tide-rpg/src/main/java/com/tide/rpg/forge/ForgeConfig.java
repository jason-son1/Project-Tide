package com.tide.rpg.forge;

import com.tide.core.reload.Reloadable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Dedicated config file for the forge (대장간) system — kept separate from
 * the plugin's main config.yml so it can be hand-edited and hot-reloaded
 * (/tide reload forge) independently of unrelated settings.
 */
public final class ForgeConfig implements Reloadable {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public ForgeConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "forge.yml");
        if (!file.exists()) {
            plugin.saveResource("forge.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public int reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
        plugin.getLogger().info("강화소 설정을 다시 불러왔습니다.");
        return 1;
    }

    public int gsPerReinforceStar() {
        return config.getInt("gs.per_reinforce_star", 5);
    }

    public int reinforceSuccessRate(int level) {
        return config.getInt("forge.reinforce-success-rate." + level, 25);
    }

    public double reinforceCostBase() {
        return config.getDouble("forge.reinforce-cost-base", 100);
    }

    public double reinforceCostMultiplier() {
        return config.getDouble("forge.reinforce-cost-multiplier", 1.5);
    }

    public int protectionScrollPearlCost() {
        return config.getInt("forge.protection-scroll-pearl-cost", 3);
    }

    public long rerollPearlCost() {
        return config.getLong("reroll.pearl-cost", 5);
    }

    public int fusionSameRuneRequired() {
        return config.getInt("fusion.same-rune-required", 3);
    }

    public long reinforceCostFor(int currentLevel) {
        return Math.round(reinforceCostBase() * Math.pow(reinforceCostMultiplier(), currentLevel));
    }
}
