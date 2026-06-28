package com.tide.mobs.boss;

import com.tide.mobs.MobKeys;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
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

        BossInstance instance = bossFightManager.getInstance(victim.getUniqueId());
        if (instance != null && instance.isShieldActive()) {
            event.setCancelled(true);
            int hitsLeft = instance.getShieldHitsLeft() - 1;
            instance.setShieldHitsLeft(hitsLeft);

            victim.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, victim.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0.1);
            victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.8f);

            if (hitsLeft <= 0) {
                instance.setShieldActive(false);
                org.bukkit.Bukkit.broadcastMessage("§a§l[기믹 클리어] §f보스의 공허 보호막이 파괴되었습니다! 보스가 3초 동안 그로기 상태에 빠집니다.");
                victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.7f);
                victim.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, victim.getLocation(), 1, 0, 0, 0, 0);

                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 4));
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 60, 4));
            } else {
                player.sendMessage("§5[보호막 타격] §f보스의 공허 보호막을 타격했습니다! §c(남은 횟수: " + hitsLeft + "회)");
            }
        }
    }

    @EventHandler
    public void onBossAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getDamager() instanceof LivingEntity attacker) || !isBoss(attacker)) {
            return;
        }
        BossInstance instance = bossFightManager.getInstance(attacker.getUniqueId());
        if (instance != null) {
            double factor = instance.getGearScoreFactor();
            event.setDamage(event.getDamage() * factor);
        }
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

    @EventHandler
    public void onDragonChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.EnderDragon) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDragonExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.EnderDragon) {
            event.setCancelled(true);
        }
    }

    private boolean isBoss(LivingEntity entity) {
        return entity.getPersistentDataContainer().get(MobKeys.BOSS_MARKER, PersistentDataType.STRING) != null;
    }
}
