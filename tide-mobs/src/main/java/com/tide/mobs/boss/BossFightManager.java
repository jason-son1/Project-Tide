package com.tide.mobs.boss;

import com.tide.core.economy.EconomyAPI;
import com.tide.mobs.MobKeys;
import com.tide.mobs.quest.BountyManager;
import com.tide.rpg.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages all active boss fights.
 * Supports VOID_KNIGHT, CORAL_QUEEN, ABYSSAL_TITAN boss types driven by SoulAltar YAML.
 * Phase pattern + enrage timer driven by a single shared 1-second tick.
 */
public final class BossFightManager {

    private static final double BASE_HP = 200;
    private static final long ENRAGE_SECONDS = 300;
    private static final double PHASE2_HP_RATIO = 0.5;
    private static final double PARTY_RADIUS = 30;

    private final JavaPlugin plugin;
    private final EconomyAPI economyAPI;
    private ItemFactory itemFactory;
    private final Map<UUID, BossInstance> activeByEntity = new ConcurrentHashMap<>();
    private BukkitTask task;
    private int patternCounter;

    public BossFightManager(JavaPlugin plugin, EconomyAPI economyAPI) {
        this.plugin = plugin;
        this.economyAPI = economyAPI;
    }

    /** Optional: provide an ItemFactory so boss drops can include custom items. */
    public void setItemFactory(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public boolean hasActiveFight(String altarId) {
        return activeByEntity.values().stream().anyMatch(i -> i.getAltarId().equals(altarId));
    }

    public void summon(Player summoner, SoulAltar altar) {
        List<Player> nearbyPlayers = summoner.getWorld().getNearbyEntities(summoner.getLocation(), PARTY_RADIUS, PARTY_RADIUS, PARTY_RADIUS)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList();

        int partySize = Math.max(1, nearbyPlayers.size());

        // Calculate average Peak Gear Score of nearby players for dynamic scaling
        double avgPeakGS = 0;
        if (!nearbyPlayers.isEmpty()) {
            double totalGS = 0;
            for (Player p : nearbyPlayers) {
                totalGS += economyAPI.getPeakGearScore(p.getUniqueId());
            }
            avgPeakGS = totalGS / nearbyPlayers.size();
        } else {
            avgPeakGS = economyAPI.getPeakGearScore(summoner.getUniqueId());
        }
        avgPeakGS = Math.max(100.0, avgPeakGS);

        // gsFactor: Baseline GS is 600.0 (factor = 1.0). Minimum factor = 0.8, Maximum factor = 3.0.
        double gsFactor = avgPeakGS / 600.0;
        if (gsFactor < 0.8) gsFactor = 0.8;
        if (gsFactor > 3.0) gsFactor = 3.0;

        EntityType bossEntityType = resolveBossEntityType(altar.getBossType());
        double hp = BASE_HP * (1.0 + 0.5 * (partySize - 1)) * bossHpMultiplier(altar.getBossType()) * gsFactor;

        LivingEntity boss = (LivingEntity) altar.summonLocation().getWorld()
                .spawnEntity(altar.summonLocation(), bossEntityType);
        var maxHealthAttribute = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(hp);
            boss.setHealth(hp);
        }
        boss.setCustomName(altar.getBossDisplayName());
        boss.setCustomNameVisible(true);
        var glowRangeManager = Bukkit.getServicesManager().load(com.tide.core.glow.GlowRangeManager.class);
        if (glowRangeManager != null) {
            glowRangeManager.register(boss, 40.0);
        } else {
            boss.setGlowing(true);
        }
        boss.getPersistentDataContainer().set(MobKeys.BOSS_MARKER, PersistentDataType.STRING, altar.getId());

        BossInstance instance = new BossInstance(altar.getId(), boss);
        instance.setBossType(altar.getBossType());
        instance.setSpawnLocation(altar.summonLocation());
        instance.setGearScoreFactor(gsFactor);
        activeByEntity.put(boss.getUniqueId(), instance);

        summoner.getWorld().strikeLightningEffect(altar.summonLocation());
        Bukkit.broadcastMessage("§4§l[보스 출현] §c" + stripColor(altar.getBossDisplayName())
                + "§c이(가) 제단에서 깨어났습니다! §7(권장 인원: " + altar.getRecommendedPartySize() + "명)");
    }

    private EntityType resolveBossEntityType(String bossType) {
        return switch (bossType.toUpperCase()) {
            case "CORAL_QUEEN" -> EntityType.ELDER_GUARDIAN;
            case "ABYSSAL_TITAN" -> EntityType.ENDER_DRAGON;
            default -> EntityType.WITHER_SKELETON; // VOID_KNIGHT
        };
    }

    private double bossHpMultiplier(String bossType) {
        return switch (bossType.toUpperCase()) {
            case "CORAL_QUEEN" -> 1.8;
            case "ABYSSAL_TITAN" -> 3.0;
            default -> 1.0;
        };
    }

    public BossInstance getInstance(UUID entityUuid) {
        return activeByEntity.get(entityUuid);
    }

    public void remove(UUID entityUuid) {
        activeByEntity.remove(entityUuid);
    }

    public void onBossDamaged(UUID entityUuid, Player attacker) {
        BossInstance instance = activeByEntity.get(entityUuid);
        if (instance != null) {
            instance.getParticipants().add(attacker.getUniqueId());
        }
    }

    public void rewardParticipants(BossInstance instance) {
        String bossType = instance.getBossType() != null ? instance.getBossType() : "VOID_KNIGHT";
        double gsFactor = instance.getGearScoreFactor();

        long clamReward = (long) (switch (bossType.toUpperCase()) {
            case "CORAL_QUEEN" -> 1500;
            case "ABYSSAL_TITAN" -> 3000;
            default -> 1000;
        } * gsFactor);

        long pearlReward = (long) (switch (bossType.toUpperCase()) {
            case "CORAL_QUEEN" -> 15;
            case "ABYSSAL_TITAN" -> 30;
            default -> 10;
        } * gsFactor);

        for (UUID participant : instance.getParticipants()) {
            economyAPI.addClam(participant, clamReward);
            economyAPI.addPearl(participant, pearlReward);
            economyAPI.addRep(participant, (int)(50 * gsFactor));
            Player player = Bukkit.getPlayer(participant);
            if (player != null) {
                player.sendTitle("§a[보스 처치]", "§f" + stripColor(instance.getEntity().getCustomName()) + "§f을(를) 쓰러뜨렸습니다!", 10, 70, 20);
                player.sendMessage("§e[보상 수령] §f기어 스코어 보정 배율 (§b" + String.format("%.2f", gsFactor) + "x§f)로 조개 §a" + clamReward + "개§f, 진주 §d" + pearlReward + "개§f를 획득했습니다.");
                BountyManager bm = Bukkit.getServicesManager().load(BountyManager.class);
                if (bm != null) {
                    bm.onBossKill(player);
                }
                // Drop custom items at the player's location
                if (itemFactory != null) {
                    try {
                        int crystalCount = (int) Math.round(1 * gsFactor);
                        for (int i = 0; i < Math.max(1, crystalCount); i++) {
                            player.getWorld().dropItemNaturally(player.getLocation(), itemFactory.create("void_crystal"));
                        }

                        if (bossType.equalsIgnoreCase("ABYSSAL_TITAN")) {
                            int coreCount = (int) Math.round(1 * gsFactor);
                            for (int i = 0; i < Math.max(1, coreCount); i++) {
                                player.getWorld().dropItemNaturally(player.getLocation(), itemFactory.create("abyssal_core"));
                            }
                        }

                        // High GearScore bonus loot
                        if (gsFactor >= 1.5) {
                            player.getWorld().dropItemNaturally(player.getLocation(), itemFactory.create("shadow_heart"));
                            player.sendMessage("§d✨ 고스펙 보너스 전리품으로 [그림자 심장] 1개를 추가 획득했습니다!");
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private void tick() {
        patternCounter++;
        for (BossInstance instance : List.copyOf(activeByEntity.values())) {
            LivingEntity entity = instance.getEntity();
            if (entity.isDead() || !entity.isValid()) {
                activeByEntity.remove(entity.getUniqueId());
                continue;
            }

            steerDragonIfApplicable(instance);

            var maxHealthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double maxHp = maxHealthAttribute != null ? maxHealthAttribute.getValue() : BASE_HP;
            double ratio = entity.getHealth() / maxHp;

            if (ratio <= PHASE2_HP_RATIO && instance.getPhase() == 1) {
                instance.setPhase(2);
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                Bukkit.broadcastMessage("§4" + stripColor(entity.getCustomName()) + "§4이(가) 2페이즈에 돌입합니다!");

                // Activate dynamic multi-hit shield for high GearScore fight
                if (instance.getGearScoreFactor() >= 1.5 && !instance.isShieldTriggered()) {
                    instance.setShieldActive(true);
                    instance.setShieldTriggered(true);
                    instance.setShieldHitsLeft(15);
                    Bukkit.broadcastMessage("§5§l[보호막 기믹] §c공허 보호막이 발동했습니다! §f(보스의 모든 피해 면역, 타격 15회 필요)");
                    entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
                }
            }

            if (instance.getElapsedSeconds() > ENRAGE_SECONDS && !instance.isEnraged()) {
                instance.setEnraged(true);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                Bukkit.broadcastMessage("§4§l[격노] §c" + stripColor(entity.getCustomName()) + "§c이(가) 격노했습니다!");
            }

            // High GS extra gimmick: Void lightning storm
            if (instance.getGearScoreFactor() >= 1.5 && patternCounter % 12 == 0) {
                spawnVoidLightning(instance);
            }

            if (patternCounter % 5 == 0) {
                String bossType = instance.getBossType() != null ? instance.getBossType() : "VOID_KNIGHT";
                switch (bossType.toUpperCase()) {
                    case "CORAL_QUEEN" -> {
                        if (instance.getPhase() == 1) coralQueenBeamPattern(entity);
                        else coralQueenSummonPattern(entity);
                    }
                    case "ABYSSAL_TITAN" -> {
                        // EnderDragon Dive Attack every 25 seconds
                        if (patternCounter % 25 == 0) {
                            abyssalTitanDivePattern(entity);
                        } else {
                            if (instance.getPhase() == 1) pullPattern(entity);
                            else abyssalTitanRagePattern(entity);
                        }
                    }
                    default -> {
                        if (instance.getPhase() == 1) pullPattern(entity);
                        else blockExplosionPattern(entity);
                    }
                }
            }
        }
    }

    /**
     * Vanilla EnderDragon AI is driven by an internal "dragon fight" controller that only
     * really exists in The End (tracking a portal, crystals, a fixed circling path) — summoned
     * standalone here, it defaults to passive holding-pattern phases and just wanders off in
     * straight lines. EnderDragon isn't a {@code Mob}, so there's no setTarget()/setAI() to hook;
     * the reliable fix is to keep forcing it back into a player-seeking combat phase, and let
     * vanilla's own flight/collision code (which already knows how to path at a player) do the
     * rest inside the now-fully-enclosed arena.
     * Also pulls or teleports the dragon back to the arena center if it flies too far away.
     */
    private void steerDragonIfApplicable(BossInstance instance) {
        LivingEntity entity = instance.getEntity();
        if (!(entity instanceof org.bukkit.entity.EnderDragon dragon)) {
            return;
        }

        var phase = dragon.getPhase();
        if (phase != org.bukkit.entity.EnderDragon.Phase.CHARGE_PLAYER && phase != org.bukkit.entity.EnderDragon.Phase.HOVER) {
            dragon.setPhase(org.bukkit.entity.EnderDragon.Phase.CHARGE_PLAYER);
        }

        // Enforce arena boundaries
        org.bukkit.Location spawnLoc = instance.getSpawnLocation();
        if (spawnLoc != null && spawnLoc.getWorld() != null) {
            double distance = dragon.getLocation().distance(spawnLoc);
            if (distance > 9.0) {
                // Sphere boundary collision: Bounce back to center
                Vector toCenter = spawnLoc.toVector().subtract(dragon.getLocation().toVector()).normalize();
                Vector currentVel = dragon.getVelocity();
                Vector bounce = currentVel.multiply(-0.3).add(toCenter.multiply(0.7));
                dragon.setVelocity(bounce);

                if (distance > 16.0) {
                    dragon.teleport(spawnLoc.clone().add(0, 2, 0));
                    dragon.setVelocity(new Vector(0, 0, 0));
                }
            } else {
                // Find closest target player in the arena
                Player target = null;
                double minDist = Double.MAX_VALUE;
                for (var nearby : dragon.getWorld().getNearbyEntities(dragon.getLocation(), 20, 20, 20)) {
                    if (nearby instanceof Player p) {
                        double d = p.getLocation().distance(dragon.getLocation());
                        if (d < minDist) {
                            minDist = d;
                            target = p;
                        }
                    }
                }
                if (target != null) {
                    // Flight interpolation LERP
                    Vector toPlayer = target.getLocation().toVector().subtract(dragon.getLocation().toVector()).normalize().multiply(0.35);
                    Vector currentVel = dragon.getVelocity();
                    Vector newVel = currentVel.multiply(0.85).add(toPlayer.multiply(0.15));
                    dragon.setVelocity(newVel);

                    // Yaw/Pitch rotation
                    Vector dir = target.getLocation().toVector().subtract(dragon.getLocation().toVector());
                    if (dir.lengthSquared() > 0.01) {
                        org.bukkit.Location lookLoc = dragon.getLocation().setDirection(dir);
                        dragon.setRotation(lookLoc.getYaw(), lookLoc.getPitch());
                    }
                }
            }
        }
    }

    /** VOID_KNIGHT Phase 1: pull nearby players */
    private void pullPattern(LivingEntity boss) {
        for (var nearby : boss.getWorld().getNearbyEntities(boss.getLocation(), 15, 15, 15)) {
            if (nearby instanceof Player player) {
                Vector pull = boss.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.6);
                player.setVelocity(pull);
            }
        }
        boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SHOOT, 1f, 0.6f);
    }

    /** VOID_KNIGHT Phase 2: near-explosion wave */
    private void blockExplosionPattern(LivingEntity boss) {
        for (var nearby : boss.getWorld().getNearbyEntities(boss.getLocation(), 20, 20, 20)) {
            if (nearby instanceof Player player) {
                player.getWorld().createExplosion(player.getLocation(), 0f, false, false);
                player.damage(4.0, boss);
            }
        }
        boss.playEffect(EntityEffect.HURT);
    }

    /** CORAL_QUEEN Phase 1: inflict Mining Fatigue on all nearby */
    private void coralQueenBeamPattern(LivingEntity boss) {
        for (var nearby : boss.getWorld().getNearbyEntities(boss.getLocation(), 18, 18, 18)) {
            if (nearby instanceof Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 1));
                player.sendMessage("§3[산호 여왕] §7저주의 가시가 당신을 휘감습니다!");
            }
        }
        boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1f);
    }

    /** CORAL_QUEEN Phase 2: summon guardian minions */
    private void coralQueenSummonPattern(LivingEntity boss) {
        boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_HURT, 1f, 0.5f);
        for (int i = 0; i < 3; i++) {
            boss.getWorld().spawnEntity(boss.getLocation().clone().add(
                    (Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4
            ), EntityType.GUARDIAN);
        }
        Bukkit.broadcastMessage("§3[산호 여왕] §f수호자들이 소환되었습니다!");
    }

    /** ABYSSAL_TITAN Phase 2: massive pull + wither effect */
    private void abyssalTitanRagePattern(LivingEntity boss) {
        for (var nearby : boss.getWorld().getNearbyEntities(boss.getLocation(), 25, 25, 25)) {
            if (nearby instanceof Player player) {
                Vector pull = boss.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2);
                player.setVelocity(pull);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
                player.sendMessage("§5[심연의 거신] §7심연의 공허가 당신을 집어삼킵니다!");
            }
        }
        boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 0.5f, 0.3f);
    }

    /** ABYSSAL_TITAN: Dive Attack Pattern */
    private void abyssalTitanDivePattern(LivingEntity boss) {
        if (!(boss instanceof org.bukkit.entity.EnderDragon dragon)) return;
        Player target = null;
        double minDist = Double.MAX_VALUE;
        for (var nearby : boss.getWorld().getNearbyEntities(boss.getLocation(), 25, 25, 25)) {
            if (nearby instanceof Player p) {
                double d = p.getLocation().distance(boss.getLocation());
                if (d < minDist) {
                    minDist = d;
                    target = p;
                }
            }
        }
        if (target == null) return;

        final org.bukkit.Location targetLoc = target.getLocation().clone();

        // 1. Rise up
        dragon.teleport(dragon.getLocation().clone().add(0, 6, 0));
        dragon.setVelocity(new Vector(0, 0, 0));
        dragon.getWorld().playSound(dragon.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.7f);
        Bukkit.broadcastMessage("§5[심연의 거신] §c심연의 거신이 급강하 공격을 준비합니다!");

        // 2. Dive down after 30 ticks
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!dragon.isValid() || dragon.isDead()) return;
            Vector diveVec = targetLoc.toVector().subtract(dragon.getLocation().toVector()).normalize().multiply(1.5);
            dragon.setVelocity(diveVec);

            // Apply crash effect after 10 ticks
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!dragon.isValid() || dragon.isDead()) return;
                dragon.teleport(targetLoc);
                dragon.setVelocity(new Vector(0, 0, 0));
                dragon.getWorld().createExplosion(targetLoc, 0.0f, false, false);
                dragon.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, targetLoc, 20, 1.0, 1.0, 1.0, 0.2);
                dragon.getWorld().playSound(targetLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);

                for (var nearby : dragon.getWorld().getNearbyEntities(targetLoc, 5, 5, 5)) {
                    if (nearby instanceof Player p) {
                        p.damage(8.0, dragon);
                        Vector knockback = p.getLocation().toVector().subtract(targetLoc.toVector()).normalize().multiply(1.2).setY(0.5);
                        p.setVelocity(knockback);
                        p.sendMessage("§c심연의 거신의 급강하 충격파에 휩쓸렸습니다!");
                    }
                }
            }, 10L);
        }, 30L);
    }

    /** High GS extra gimmick: Void lightning storm */
    private void spawnVoidLightning(BossInstance instance) {
        LivingEntity boss = instance.getEntity();
        List<Player> players = new java.util.ArrayList<>();
        for (var nearby : boss.getWorld().getNearbyEntities(boss.getLocation(), 25, 25, 25)) {
            if (nearby instanceof Player p) {
                players.add(p);
            }
        }
        if (players.isEmpty()) return;

        Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        org.bukkit.Location targetLoc = target.getLocation().clone();

        Bukkit.getScheduler().runTaskTimer(plugin, new java.util.function.Consumer<org.bukkit.scheduler.BukkitTask>() {
            int elapsed = 0;
            @Override
            public void accept(org.bukkit.scheduler.BukkitTask task) {
                if (elapsed >= 8) {
                    task.cancel();
                    if (boss.isValid() && !boss.isDead()) {
                        targetLoc.getWorld().strikeLightningEffect(targetLoc);
                        for (var nearby : targetLoc.getWorld().getNearbyEntities(targetLoc, 3.0, 3.0, 3.0)) {
                            if (nearby instanceof Player p) {
                                p.damage(5.0 * instance.getGearScoreFactor(), boss);
                                p.sendMessage("§5[공허 번개] §c바닥의 공허 벼락을 피하지 못했습니다!");
                            }
                        }
                    }
                    return;
                }
                targetLoc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, targetLoc, 15, 0.5, 0.1, 0.5, 0.05);
                elapsed++;
            }
        }, 0L, 5L);
    }

    private static String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}


