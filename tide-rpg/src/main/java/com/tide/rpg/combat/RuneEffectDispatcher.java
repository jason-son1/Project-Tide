package com.tide.rpg.combat;

import com.tide.core.tide.TideState;
import com.tide.core.tide.TideStateProvider;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies the exact rune formulas from the 기획 docs. Grade always comes
 * straight from the "type:grade" PDC socket string — no lookups required
 * for the gameplay math, only RuneRegistry needs the yml (for Lore names).
 *
 * Temporal Rune Awakening (2-3): lightning awakens during SPRING_TIDE/BLOOD_TIDE
 * (double chance + chain to nearby targets); lifesteal awakens during
 * BLOOD_MOON/BLOOD_TIDE (flat true-damage heal instead of a % of damage dealt).
 */
public final class RuneEffectDispatcher {

    private final TideStateProvider tideStateProvider;

    public RuneEffectDispatcher(TideStateProvider tideStateProvider) {
        this.tideStateProvider = tideStateProvider;
    }

    public void dispatch(String type, int grade, Player attacker, LivingEntity victim, double finalDamage) {
        switch (type.toLowerCase()) {
            case "lifesteal" -> lifesteal(attacker, grade, finalDamage);
            case "lightning" -> lightning(victim, grade);
            case "slow" -> slow(victim, grade);
            case "berserk" -> { /* damage bonus is applied pre-hit in CombatListener */ }
            default -> { /* shield is defensive-side; handled by DefensiveListener */ }
        }
    }

    private boolean isAwakeningState(TideState a, TideState b) {
        TideState current = tideStateProvider.getCurrentState();
        return current == a || current == b;
    }

    private void lifesteal(Player attacker, int grade, double finalDamage) {
        boolean awakened = isAwakeningState(TideState.BLOOD_MOON, TideState.BLOOD_TIDE);
        double healAmount = awakened ? 2.0 * grade : finalDamage * (0.08 * grade);
        double maxHealth = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + healAmount));
    }

    private void lightning(LivingEntity victim, int grade) {
        boolean awakened = isAwakeningState(TideState.SPRING_TIDE, TideState.BLOOD_TIDE);
        double chance = Math.min(1.0, 0.10 * grade * (awakened ? 2.0 : 1.0));
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            victim.getWorld().strikeLightningEffect(victim.getLocation());
            victim.damage(4.0);

            if (awakened) {
                double chainRadius = 3.0 * 2.0; // 체인 라이트닝 범위 +100%
                int chained = 0;
                for (Entity nearby : victim.getNearbyEntities(chainRadius, chainRadius, chainRadius)) {
                    if (chained >= 2) {
                        break;
                    }
                    if (nearby instanceof LivingEntity other && !other.equals(victim)) {
                        other.getWorld().strikeLightningEffect(other.getLocation());
                        other.damage(2.0);
                        chained++;
                    }
                }
            }
        }
    }

    private void slow(LivingEntity victim, int grade) {
        int durationTicks = 40 + (grade * 20);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, Math.max(0, grade - 1)));
    }

    public double berserkDamageBonus() {
        return 0.30;
    }

    public double berserkReceivedPenalty() {
        return 0.15;
    }
}
