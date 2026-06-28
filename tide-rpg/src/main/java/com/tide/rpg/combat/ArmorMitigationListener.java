package com.tide.rpg.combat;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

/**
 * Vanilla armor only mitigates direct entity-attack damage; causes like POISON,
 * WITHER, MAGIC and fire ignore armor entirely and land as near-true-damage. That
 * makes a fully-geared player just as fragile to a poison potion as a naked one,
 * which undercuts the whole point of investing GS into armor. This retrofits a
 * percentage-based mitigation onto a configured set of "natural damage" causes,
 * scaled by the player's current GENERIC_ARMOR attribute value.
 */
public final class ArmorMitigationListener implements Listener {

    private final JavaPlugin plugin;

    public ArmorMitigationListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("armor-mitigation.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!mitigatedCauses().contains(event.getCause())) {
            return;
        }

        var armorAttribute = player.getAttribute(Attribute.GENERIC_ARMOR);
        if (armorAttribute == null) {
            return;
        }
        double armor = armorAttribute.getValue();
        if (armor <= 0) {
            return;
        }

        double perPoint = plugin.getConfig().getDouble("armor-mitigation.per-point-reduction", 0.04);
        double maxReduction = plugin.getConfig().getDouble("armor-mitigation.max-reduction", 0.6);
        double reduction = Math.min(maxReduction, armor * perPoint);
        event.setDamage(event.getDamage() * (1.0 - reduction));
    }

    private Set<EntityDamageEvent.DamageCause> mitigatedCauses() {
        Set<EntityDamageEvent.DamageCause> causes = new HashSet<>();
        for (String name : plugin.getConfig().getStringList("armor-mitigation.causes")) {
            try {
                causes.add(EntityDamageEvent.DamageCause.valueOf(name));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return causes;
    }
}
