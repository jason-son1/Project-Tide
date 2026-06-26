package com.tide.mobs.affix;

import com.tide.rpg.TideKeys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Affix combat behaviors, implemented first for [염화의] per the stage-1 plan.
 * Other affixes ([신속의], [폭심의] ...) get their own handlers as later affixes land.
 */
public final class AffixCombatListener implements Listener {

    private static final String FLAME_AFFIX_ID = "염화의";
    private static final double FLAME_TRIGGER_CHANCE = 0.30;

    private final org.bukkit.plugin.Plugin plugin;

    public AffixCombatListener(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEliteHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) {
            return;
        }
        String affixes = attacker.getPersistentDataContainer().get(TideKeys.AFFIXES, PersistentDataType.STRING);
        if (affixes == null || !affixes.contains(FLAME_AFFIX_ID)) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= FLAME_TRIGGER_CHANCE) {
            return;
        }

        Location center = event.getEntity().getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = center.clone().add(dx, 0, dz).getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(Material.FIRE);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (block.getType() == Material.FIRE) {
                            block.setType(Material.AIR);
                        }
                    }, 40L);
                }
            }
        }
    }
}
