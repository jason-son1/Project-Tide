package com.tide.mobs.nemesis;

import com.tide.rpg.TideKeys;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Two awakening triggers per the spec: (1) the elite actually kills the
 * player, or (2) the player drops below 5% HP to that elite and then
 * survives (i.e. fled instead of finishing the fight).
 */
public final class NemesisTriggerListener implements Listener {

    private final JavaPlugin plugin;
    private final NemesisManager nemesisManager;
    private final CalamityManager calamityManager;
    private final LegacyTheftManager legacyTheftManager;

    public NemesisTriggerListener(JavaPlugin plugin, NemesisManager nemesisManager, CalamityManager calamityManager,
                                   LegacyTheftManager legacyTheftManager) {
        this.plugin = plugin;
        this.nemesisManager = nemesisManager;
        this.calamityManager = calamityManager;
        this.legacyTheftManager = legacyTheftManager;
    }

    /**
     * LOW so this runs before TideCore's WreckageGrave handler (NORMAL) —
     * Legacy Theft (3-2) needs to pull the best item out of event.getDrops()
     * before the grave snapshot is taken.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        var lastDamage = player.getLastDamageCause();
        if (!(lastDamage instanceof EntityDamageByEntityEvent damageEvent)) {
            return;
        }
        if (!(damageEvent.getDamager() instanceof LivingEntity mob)) {
            return;
        }
        if (isElite(mob)) {
            legacyTheftManager.stealBestItem(mob, player, event.getDrops());
        }
        tryAwaken(mob, player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.isCancelled()) {
            return;
        }
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)) {
            return;
        }
        if (!(damageEvent.getDamager() instanceof LivingEntity mob) || !isElite(mob)) {
            return;
        }

        var maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20;
        double healthAfter = player.getHealth() - event.getFinalDamage();
        if (healthAfter / maxHealth >= 0.05 || healthAfter <= 0) {
            return; // not a near-death moment, or this hit is fatal (handled by onPlayerDeath instead)
        }

        UUID mobUuid = mob.getUniqueId();
        UUID playerUuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player onlinePlayer = Bukkit.getPlayer(playerUuid);
            LivingEntity liveMob = (LivingEntity) Bukkit.getEntity(mobUuid);
            if (onlinePlayer == null || !onlinePlayer.isOnline() || onlinePlayer.isDead()) {
                return;
            }
            if (liveMob == null || liveMob.isDead() || !liveMob.isValid()) {
                return;
            }
            tryAwaken(liveMob, onlinePlayer);
        }, 60L);
    }

    private void tryAwaken(LivingEntity mob, Player player) {
        if (!isElite(mob)) {
            return;
        }
        String affixes = mob.getPersistentDataContainer().get(TideKeys.AFFIXES, PersistentDataType.STRING);
        NemesisRecord record = nemesisManager.awaken(mob, player, affixes);
        if (record != null && calamityManager.isCalamityEligible(record)) {
            calamityManager.evolve(mob, record);
        }
    }

    private boolean isElite(LivingEntity entity) {
        return entity.getPersistentDataContainer().getOrDefault(TideKeys.ELITE, PersistentDataType.BYTE, (byte) 0) == 1;
    }
}
