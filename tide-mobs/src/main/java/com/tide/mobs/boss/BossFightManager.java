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
        int partySize = (int) summoner.getWorld().getNearbyEntities(summoner.getLocation(), PARTY_RADIUS, PARTY_RADIUS, PARTY_RADIUS)
                .stream().filter(e -> e instanceof Player).count();
        partySize = Math.max(1, partySize);

        EntityType bossEntityType = resolveBossEntityType(altar.getBossType());
        double hp = BASE_HP * (1 + 0.5 * (partySize - 1)) * bossHpMultiplier(altar.getBossType());

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
        long clamReward = switch (bossType.toUpperCase()) {
            case "CORAL_QUEEN" -> 1500;
            case "ABYSSAL_TITAN" -> 3000;
            default -> 1000;
        };
        long pearlReward = switch (bossType.toUpperCase()) {
            case "CORAL_QUEEN" -> 15;
            case "ABYSSAL_TITAN" -> 30;
            default -> 10;
        };

        for (UUID participant : instance.getParticipants()) {
            economyAPI.addClam(participant, clamReward);
            economyAPI.addPearl(participant, pearlReward);
            economyAPI.addRep(participant, 50);
            Player player = Bukkit.getPlayer(participant);
            if (player != null) {
                player.sendTitle("§a[보스 처치]", "§f" + stripColor(instance.getEntity().getCustomName()) + "§f을(를) 쓰러뜨렸습니다!", 10, 70, 20);
                BountyManager bm = Bukkit.getServicesManager().load(BountyManager.class);
                if (bm != null) {
                    bm.onBossKill(player);
                }
                // Drop custom items at the player's location
                if (itemFactory != null) {
                    try {
                        player.getWorld().dropItemNaturally(player.getLocation(), itemFactory.create("void_crystal"));
                        if (bossType.equalsIgnoreCase("ABYSSAL_TITAN")) {
                            player.getWorld().dropItemNaturally(player.getLocation(), itemFactory.create("abyssal_core"));
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

            var maxHealthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double maxHp = maxHealthAttribute != null ? maxHealthAttribute.getValue() : BASE_HP;
            double ratio = entity.getHealth() / maxHp;

            if (ratio <= PHASE2_HP_RATIO && instance.getPhase() == 1) {
                instance.setPhase(2);
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                Bukkit.broadcastMessage("§4" + stripColor(entity.getCustomName()) + "§4이(가) 2페이즈에 돌입합니다!");
            }

            if (instance.getElapsedSeconds() > ENRAGE_SECONDS && !instance.isEnraged()) {
                instance.setEnraged(true);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                Bukkit.broadcastMessage("§4§l[격노] §c" + stripColor(entity.getCustomName()) + "§c이(가) 격노했습니다!");
            }

            if (patternCounter % 5 == 0) {
                String bossType = instance.getBossType() != null ? instance.getBossType() : "VOID_KNIGHT";
                switch (bossType.toUpperCase()) {
                    case "CORAL_QUEEN" -> {
                        if (instance.getPhase() == 1) coralQueenBeamPattern(entity);
                        else coralQueenSummonPattern(entity);
                    }
                    case "ABYSSAL_TITAN" -> {
                        if (instance.getPhase() == 1) pullPattern(entity);
                        else abyssalTitanRagePattern(entity);
                    }
                    default -> {
                        if (instance.getPhase() == 1) pullPattern(entity);
                        else blockExplosionPattern(entity);
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

    private static String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}


