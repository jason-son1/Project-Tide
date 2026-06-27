package com.tide.mobs.affix;

import com.tide.mobs.MobKeys;
import com.tide.rpg.TideKeys;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Applied exactly once, from CreatureSpawnEvent — never from a tick loop.
 * Multiplier bonuses stack additively ("합산"): two affixes each at 1.5x
 * combine to 2.0x, not 2.25x.
 */
public final class EliteProcessor {

    public void apply(LivingEntity entity, List<AffixDefinition> affixes) {
        if (affixes.isEmpty()) {
            return;
        }

        double hpMultiplier = 1.0;
        double damageMultiplier = 1.0;
        for (AffixDefinition affix : affixes) {
            hpMultiplier += affix.getHpMultiplier() - 1.0;
            damageMultiplier += affix.getDamageMultiplier() - 1.0;
        }

        var maxHealthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double newMax = maxHealthAttribute.getBaseValue() * hpMultiplier;
            maxHealthAttribute.setBaseValue(newMax);
            entity.setHealth(newMax);
        }

        String namePrefix = affixes.stream()
                .map(AffixDefinition::getDisplayName)
                .collect(Collectors.joining(""));
        entity.setCustomName(namePrefix + " §f" + baseDisplayName(entity));
        entity.setCustomNameVisible(true);
        var glowRangeManager = org.bukkit.Bukkit.getServicesManager().load(com.tide.core.glow.GlowRangeManager.class);
        if (glowRangeManager != null) {
            glowRangeManager.register(entity, 20.0);
        } else {
            entity.setGlowing(true);
        }

        var pdc = entity.getPersistentDataContainer();
        pdc.set(TideKeys.ELITE, PersistentDataType.BYTE, (byte) 1);
        pdc.set(TideKeys.AFFIXES, PersistentDataType.STRING,
                affixes.stream().map(AffixDefinition::getId).collect(Collectors.joining(",")));
        pdc.set(MobKeys.DAMAGE_MULTIPLIER, PersistentDataType.DOUBLE, damageMultiplier);

        for (AffixDefinition affix : affixes) {
            if (affix.getSpawnParticle() != null) {
                entity.getWorld().spawnParticle(affix.getSpawnParticle(), entity.getLocation(), 20, 0.5, 1, 0.5);
            }
        }
    }

    private String baseDisplayName(LivingEntity entity) {
        return entity.getType().name().toLowerCase().replace('_', ' ');
    }
}
