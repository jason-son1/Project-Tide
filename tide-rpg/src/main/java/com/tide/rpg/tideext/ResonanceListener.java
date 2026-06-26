package com.tide.rpg.tideext;

import com.tide.core.tide.TideStateProvider;
import com.tide.rpg.TideKeys;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Tide Resonance Set (2-1): a weapon/armor piece tagged tide:resonance="HIGH_TIDE"
 * etc. gets +50% effect only while that exact TideState is active — otherwise
 * it behaves like a plain T2 item. Checked live, not cached, so swapping gear
 * mid-fight changes the outcome immediately.
 */
public final class ResonanceListener implements Listener {

    public static final double RESONANCE_BONUS = 0.50;

    private final TideStateProvider tideStateProvider;

    public ResonanceListener(TideStateProvider tideStateProvider) {
        this.tideStateProvider = tideStateProvider;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (matchesCurrentTide(weapon)) {
            event.setDamage(event.getDamage() * (1.0 + RESONANCE_BONUS));
            if (event.getEntity() instanceof LivingEntity victim) {
                victim.getWorld().spawnParticle(Particle.SPLASH, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDefend(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        boolean anyResonantArmor = false;
        for (ItemStack armorPiece : victim.getInventory().getArmorContents()) {
            if (matchesCurrentTide(armorPiece)) {
                anyResonantArmor = true;
                break;
            }
        }
        if (anyResonantArmor) {
            event.setDamage(event.getDamage() / (1.0 + RESONANCE_BONUS));
        }
    }

    private boolean matchesCurrentTide(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        String resonance = meta.getPersistentDataContainer().get(TideKeys.RESONANCE, PersistentDataType.STRING);
        return resonance != null && resonance.equalsIgnoreCase(tideStateProvider.getCurrentState().name());
    }
}
