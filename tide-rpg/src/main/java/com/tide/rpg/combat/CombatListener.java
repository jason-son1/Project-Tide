package com.tide.rpg.combat;

import com.tide.rpg.TideKeys;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads the attacker's main-hand weapon sockets on every hit and routes them
 * through RuneEffectDispatcher. Pure event-driven — no per-tick scanning.
 */
public final class CombatListener implements Listener {

    private final RuneEffectDispatcher dispatcher;

    public CombatListener(RuneEffectDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttackDamageModify(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (hasSocketOfType(pdc, "berserk")) {
            event.setDamage(event.getDamage() * (1.0 + dispatcher.berserkDamageBonus()));
        }
        if (SetBonus.isActive(pdc)) {
            event.setDamage(event.getDamage() * (1.0 + SetBonus.DAMAGE_BONUS));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAttackEffects(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int socketCount = pdc.getOrDefault(TideKeys.SOCKET_COUNT, PersistentDataType.INTEGER, 0);
        double finalDamage = event.getFinalDamage();

        for (int i = 1; i <= socketCount; i++) {
            String raw = pdc.get(TideKeys.socket(i), PersistentDataType.STRING);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] parts = raw.split(":");
            if (parts.length != 2) {
                continue;
            }
            try {
                int grade = Integer.parseInt(parts[1]);
                dispatcher.dispatch(parts[0], grade, attacker, victim, finalDamage);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private boolean hasSocketOfType(PersistentDataContainer pdc, String type) {
        int socketCount = pdc.getOrDefault(TideKeys.SOCKET_COUNT, PersistentDataType.INTEGER, 0);
        for (int i = 1; i <= socketCount; i++) {
            String raw = pdc.get(TideKeys.socket(i), PersistentDataType.STRING);
            if (raw != null && raw.toLowerCase().startsWith(type.toLowerCase() + ":")) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatHealthDisplay(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) {
            return;
        }

        double maxHp = victim.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHp = Math.max(0.0, victim.getHealth() - event.getFinalDamage());

        String name = victim.getCustomName() != null ? victim.getCustomName() : victim.getType().name().replace("_", " ");
        double ratio = currentHp / maxHp;
        int filled = (int) Math.round(ratio * 10.0);
        filled = Math.max(0, Math.min(10, filled));

        StringBuilder bar = new StringBuilder();
        bar.append("§c");
        for (int i = 0; i < filled; i++) {
            bar.append("■");
        }
        bar.append("§7");
        for (int i = filled; i < 10; i++) {
            bar.append("■");
        }

        String message = String.format("§6§l%s §7| §f%.1f §7/ §f%.1f HP §7[%s§7]", name, currentHp, maxHp, bar.toString());
        attacker.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(message));
    }
}
