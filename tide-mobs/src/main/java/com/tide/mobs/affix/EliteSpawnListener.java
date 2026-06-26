package com.tide.mobs.affix;

import com.tide.core.tide.TideState;
import com.tide.core.tide.TideStateProvider;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rolls elite-ification exactly once, at spawn time. The chance depends on
 * the current tide state via TideStateProvider (read, not polled — this
 * listener only runs when a CreatureSpawnEvent actually fires).
 */
public final class EliteSpawnListener implements Listener {

    private final TideStateProvider tideStateProvider;
    private final AffixRegistry affixRegistry;
    private final EliteProcessor eliteProcessor;

    public EliteSpawnListener(TideStateProvider tideStateProvider, AffixRegistry affixRegistry,
                               EliteProcessor eliteProcessor) {
        this.tideStateProvider = tideStateProvider;
        this.affixRegistry = affixRegistry;
        this.eliteProcessor = eliteProcessor;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }
        LivingEntity entity = event.getEntity();

        double chance = eliteChance(tideStateProvider.getCurrentState());
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        List<AffixDefinition> pool = affixRegistry.all();
        if (pool.isEmpty()) {
            return;
        }
        List<AffixDefinition> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        int affixCount = Math.min(shuffled.size(), ThreadLocalRandom.current().nextBoolean() ? 1 : 2);
        eliteProcessor.apply(entity, shuffled.subList(0, affixCount));
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
