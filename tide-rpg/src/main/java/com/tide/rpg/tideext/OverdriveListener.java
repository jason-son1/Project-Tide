package com.tide.rpg.tideext;

import com.tide.core.tide.TideStateProvider;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Tidal Overdrive (2-2): build a gauge by hitting things while wearing any
 * Resonance-tagged gear, then unleash it with Shift + right-click. The burst
 * shape depends on the *current* TideState, not which Resonance the gear had.
 */
public final class OverdriveListener implements Listener {

    private static final double BURST_RADIUS = 5.0;

    private final OverdriveManager overdriveManager;
    private final TideStateProvider tideStateProvider;

    public OverdriveListener(OverdriveManager overdriveManager, TideStateProvider tideStateProvider) {
        this.overdriveManager = overdriveManager;
        this.tideStateProvider = tideStateProvider;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (overdriveManager.isWearingResonance(attacker)) {
            overdriveManager.addGauge(attacker, event.getFinalDamage());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking() || event.getHand() == null) {
            return;
        }
        if (!overdriveManager.isFull(player)) {
            return;
        }
        overdriveManager.consume(player);
        unleash(player);
    }

    private void unleash(Player player) {
        Location center = player.getLocation();
        switch (tideStateProvider.getCurrentState()) {
            case HIGH_TIDE -> {
                for (LivingEntity target : nearbyEnemies(player)) {
                    Vector knockback = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.5).setY(0.3);
                    target.setVelocity(knockback);
                    target.damage(3.0, player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                }
                player.getWorld().spawnParticle(Particle.SPLASH, center, 60, 2, 1, 2);
                player.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH, 1.5f, 0.8f);
            }
            case BLOOD_MOON, BLOOD_TIDE -> {
                double totalDamage = 0;
                for (LivingEntity target : nearbyEnemies(player)) {
                    target.damage(8.0, player);
                    totalDamage += 8.0;
                }
                for (var ally : player.getWorld().getNearbyEntities(center, BURST_RADIUS, BURST_RADIUS, BURST_RADIUS)) {
                    if (ally instanceof Player allyPlayer) {
                        var maxHealthAttr = allyPlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20;
                        allyPlayer.setHealth(Math.min(maxHealth, allyPlayer.getHealth() + totalDamage));
                    }
                }
                player.getWorld().spawnParticle(Particle.DRIPPING_LAVA, center, 40, 2, 1, 2);
                player.getWorld().playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 1.5f);
            }
            default -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                player.getWorld().spawnParticle(Particle.END_ROD, center, 40, 1, 1, 1);
            }
        }
    }

    private Iterable<LivingEntity> nearbyEnemies(Player player) {
        java.util.List<LivingEntity> enemies = new java.util.ArrayList<>();
        for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), BURST_RADIUS, BURST_RADIUS, BURST_RADIUS)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
                enemies.add(living);
            }
        }
        return enemies;
    }
}
