package com.tide.rpg.combat;

import com.tide.rpg.TideKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Wearer-side rune effects: shield (absorption on hit) and the berserk curse's
 * "+15% damage received" penalty. Triggered on EntityDamageEvent since these
 * react to the victim being hit, regardless of damage source.
 */
public final class DefensiveListener implements Listener {

    private final RuneEffectDispatcher dispatcher;

    public DefensiveListener(RuneEffectDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (hasWeaponSocketOfType(victim, "berserk")) {
            event.setDamage(event.getDamage() * (1.0 + dispatcher.berserkReceivedPenalty()));
        }

        int shieldGrade = highestArmorSocketGrade(victim, "shield");
        if (shieldGrade > 0) {
            victim.setAbsorptionAmount(5.0 * shieldGrade);
        }
    }

    private boolean hasWeaponSocketOfType(Player player, String type) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) {
            return false;
        }
        return socketGradeOfType(meta.getPersistentDataContainer(), type) > 0;
    }

    private int highestArmorSocketGrade(Player player, String type) {
        int highest = 0;
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece == null) {
                continue;
            }
            ItemMeta meta = armorPiece.getItemMeta();
            if (meta == null) {
                continue;
            }
            highest = Math.max(highest, socketGradeOfType(meta.getPersistentDataContainer(), type));
        }
        return highest;
    }

    private int socketGradeOfType(PersistentDataContainer pdc, String type) {
        int socketCount = pdc.getOrDefault(TideKeys.SOCKET_COUNT, PersistentDataType.INTEGER, 0);
        for (int i = 1; i <= socketCount; i++) {
            String raw = pdc.get(TideKeys.socket(i), PersistentDataType.STRING);
            if (raw == null) {
                continue;
            }
            String[] parts = raw.split(":");
            if (parts.length == 2 && parts[0].equalsIgnoreCase(type)) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }
}
