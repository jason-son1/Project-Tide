package com.tide.mobs.difficulty;

import com.tide.core.difficulty.DifficultyResult;
import com.tide.mobs.MobKeys;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

/**
 * Applies the World-Difficulty-Multiplier (Dynamic World Difficulty Scaling) to a
 * mob's HP/attack-damage attributes idempotently: the previously-applied multiplier
 * is recorded in PDC and divided back out before the new one is multiplied in, so
 * calling this repeatedly (spawn time + periodic refresh) never compounds on top of
 * itself. Any other authored multiplier (mob's own hp-multiplier, elite affixes)
 * must already be baked into the attribute's base value before the first call.
 */
public final class WorldDifficultyApplier {

    private WorldDifficultyApplier() {
    }

    public static void apply(LivingEntity entity, DifficultyResult difficulty) {
        var pdc = entity.getPersistentDataContainer();
        double prevHpMult = pdc.getOrDefault(MobKeys.WDM_HP_MULT, PersistentDataType.DOUBLE, 1.0);
        double prevDmgMult = pdc.getOrDefault(MobKeys.WDM_DMG_MULT, PersistentDataType.DOUBLE, 1.0);

        AttributeInstance hpAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hpAttribute != null) {
            double unscaledBase = hpAttribute.getBaseValue() / prevHpMult;
            double newMax = unscaledBase * difficulty.hpMultiplier();
            double healthRatio = entity.getHealth() / hpAttribute.getBaseValue();
            hpAttribute.setBaseValue(newMax);
            entity.setHealth(Math.max(1.0, Math.min(newMax, newMax * healthRatio)));
        }

        AttributeInstance dmgAttribute = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmgAttribute != null) {
            double unscaledBase = dmgAttribute.getBaseValue() / prevDmgMult;
            dmgAttribute.setBaseValue(unscaledBase * difficulty.dmgMultiplier());
        }

        pdc.set(MobKeys.WDM_HP_MULT, PersistentDataType.DOUBLE, difficulty.hpMultiplier());
        pdc.set(MobKeys.WDM_DMG_MULT, PersistentDataType.DOUBLE, difficulty.dmgMultiplier());
    }
}
