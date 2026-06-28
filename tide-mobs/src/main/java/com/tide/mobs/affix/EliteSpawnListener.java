package com.tide.mobs.affix;

import com.tide.core.difficulty.DifficultyBracket;
import com.tide.core.difficulty.DifficultyManager;
import com.tide.core.difficulty.DifficultyResult;
import com.tide.core.tide.TideState;
import com.tide.core.tide.TideStateProvider;
import com.tide.mobs.difficulty.WorldDifficultyApplier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rolls elite-ification exactly once, at spawn time. The chance depends on
 * the current tide state via TideStateProvider (read, not polled — this
 * listener only runs when a CreatureSpawnEvent actually fires). Also applies
 * the world-difficulty multiplier (Spec: Dynamic World Difficulty Scaling)
 * to every natural spawn, elite or not, and re-weights/forces affix
 * selection per the resolved PI bracket.
 */
public final class EliteSpawnListener implements Listener {

    private final JavaPlugin plugin;
    private final TideStateProvider tideStateProvider;
    private final AffixRegistry affixRegistry;
    private final EliteProcessor eliteProcessor;
    private final DifficultyManager difficultyManager;

    public EliteSpawnListener(JavaPlugin plugin, TideStateProvider tideStateProvider, AffixRegistry affixRegistry,
                               EliteProcessor eliteProcessor, DifficultyManager difficultyManager) {
        this.plugin = plugin;
        this.tideStateProvider = tideStateProvider;
        this.affixRegistry = affixRegistry;
        this.eliteProcessor = eliteProcessor;
        this.difficultyManager = difficultyManager;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }
        LivingEntity entity = event.getEntity();

        DifficultyResult difficulty = null;
        if (difficultyManager != null && difficultyManager.isEnabled()) {
            difficulty = difficultyManager.resolve(entity.getLocation());
            applyWorldDifficulty(entity, difficulty);
        }

        double chance = eliteChance(tideStateProvider.getCurrentState());
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        List<AffixDefinition> pool = affixRegistry.all();
        if (pool.isEmpty()) {
            return;
        }
        DifficultyBracket bracket = difficulty != null ? difficulty.bracket() : null;
        List<AffixDefinition> chosen = selectAffixes(pool, bracket);
        if (chosen.isEmpty()) {
            return;
        }
        eliteProcessor.apply(entity, chosen);

        com.tide.core.effect.EffectEngine effectEngine = org.bukkit.Bukkit.getServicesManager().load(com.tide.core.effect.EffectEngine.class);
        if (effectEngine != null) {
            effectEngine.playEffect(entity.getLocation(), "elite_spawn");
        }
    }

    /** Scales base HP/attack-damage by the resolved world-difficulty multiplier — applies to every natural spawn. */
    private void applyWorldDifficulty(LivingEntity entity, DifficultyResult difficulty) {
        WorldDifficultyApplier.apply(entity, difficulty);
    }

    private int affixCountFor(DifficultyBracket bracket) {
        if (bracket == null) {
            return ThreadLocalRandom.current().nextBoolean() ? 1 : 2;
        }
        return Math.max(1, bracket.maxAffixes());
    }

    /**
     * T4-T5 brackets force a guaranteed synergy combo (e.g. split+explosive)
     * when fully configured; otherwise affixes are drawn by weighted random
     * sampling without replacement, with bracket-specific weight overrides
     * (e.g. T2-T3 favoring flame/thorns/shield) layered on the base weight.
     */
    private List<AffixDefinition> selectAffixes(List<AffixDefinition> pool, DifficultyBracket bracket) {
        if (bracket != null) {
            List<String> synergyIds = plugin.getConfig().getStringList("difficulty-scaling.guaranteed-synergy." + bracket.id());
            if (!synergyIds.isEmpty()) {
                List<AffixDefinition> combo = new ArrayList<>();
                for (String id : synergyIds) {
                    for (AffixDefinition def : pool) {
                        if (def.getId().equalsIgnoreCase(id)) {
                            combo.add(def);
                            break;
                        }
                    }
                }
                if (combo.size() == synergyIds.size()) {
                    return combo;
                }
            }
        }

        int count = Math.min(affixCountFor(bracket), pool.size());
        ConfigurationSection overrides = bracket == null ? null
                : plugin.getConfig().getConfigurationSection("difficulty-scaling.affix-weight-overrides." + bracket.id());

        List<AffixDefinition> remaining = new ArrayList<>(pool);
        List<AffixDefinition> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double totalWeight = 0;
            for (AffixDefinition def : remaining) {
                totalWeight += effectiveWeight(def, overrides);
            }
            if (totalWeight <= 0) {
                break;
            }
            double roll = ThreadLocalRandom.current().nextDouble() * totalWeight;
            double cumulative = 0;
            AffixDefinition chosen = remaining.get(remaining.size() - 1);
            for (AffixDefinition def : remaining) {
                cumulative += effectiveWeight(def, overrides);
                if (roll < cumulative) {
                    chosen = def;
                    break;
                }
            }
            result.add(chosen);
            remaining.remove(chosen);
        }
        return result;
    }

    private double effectiveWeight(AffixDefinition def, ConfigurationSection overrides) {
        double multiplier = overrides == null ? 1.0 : overrides.getDouble(def.getId(), 1.0);
        return def.getWeight() * multiplier;
    }

    private double eliteChance(TideState state) {
        return switch (state) {
            case HIGH_TIDE -> 0.05;
            case LOW_TIDE -> 0.12;
            case SPRING_TIDE -> 0.20;
            case BLOOD_MOON -> 0.25;
            case BLOOD_TIDE -> 0.30;
        };
    }
}
