package com.tide.rpg.combat;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies the exact rune formulas from the 기획 docs. Grade always comes
 * straight from the "type:grade" PDC socket string — no lookups required
 * for the gameplay math, only RuneRegistry needs the yml (for Lore names).
 */
public final class RuneEffectDispatcher {

    public void dispatch(String type, int grade, Player attacker, LivingEntity victim, double finalDamage) {
        switch (type.toLowerCase()) {
            case "lifesteal" -> lifesteal(attacker, grade, finalDamage);
            case "lightning" -> lightning(victim, grade);
            case "slow" -> slow(victim, grade);
            case "berserk" -> { /* damage bonus is applied pre-hit in CombatListener */ }
            default -> { /* shield is defensive-side; handled by DefensiveListener */ }
        }
    }

    private void lifesteal(Player attacker, int grade, double finalDamage) {
        double healAmount = finalDamage * (0.08 * grade);
        double maxHealth = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + healAmount));
    }

    private void lightning(LivingEntity victim, int grade) {
        double chance = 0.10 * grade;
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            victim.getWorld().strikeLightningEffect(victim.getLocation());
            victim.damage(4.0);
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
