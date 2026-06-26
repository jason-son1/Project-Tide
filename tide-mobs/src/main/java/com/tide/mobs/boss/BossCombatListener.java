package com.tide.mobs.boss;

import com.tide.mobs.MobKeys;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public final class BossCombatListener implements Listener {

    private final BossFightManager bossFightManager;

    public BossCombatListener(BossFightManager bossFightManager) {
        this.bossFightManager = bossFightManager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim) || !isBoss(victim)) {
            return;
        }
        bossFightManager.onBossDamaged(victim.getUniqueId(), player);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isBoss(event.getEntity())) {
            return;
        }
        BossInstance instance = bossFightManager.getInstance(event.getEntity().getUniqueId());
        if (instance == null) {
            return;
        }
        bossFightManager.rewardParticipants(instance);
        bossFightManager.remove(event.getEntity().getUniqueId());
    }

    private boolean isBoss(LivingEntity entity) {
        return entity.getPersistentDataContainer().get(MobKeys.BOSS_MARKER, PersistentDataType.STRING) != null;
    }
}
