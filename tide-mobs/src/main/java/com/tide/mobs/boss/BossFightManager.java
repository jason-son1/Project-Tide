package com.tide.mobs.boss;

import com.tide.core.economy.EconomyAPI;
import com.tide.mobs.MobKeys;
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
 * "공허의 기사" — 1 boss type for now (the 보스 로스터 3종 + 월드보스 from the
 * doc are a content-volume goal for the 3파트 generator, not a 1파트 engine
 * feature). Phase pattern + enrage timer are driven by a single shared
 * 1-second tick over the (at most a few) active instances — not a scan of
 * all entities.
 */
public final class BossFightManager {

    private static final double BASE_HP = 200;
    private static final long ENRAGE_SECONDS = 300;
    private static final double PHASE2_HP_RATIO = 0.5;
    private static final double PARTY_RADIUS = 30;

    private final JavaPlugin plugin;
    private final EconomyAPI economyAPI;
    private final Map<UUID, BossInstance> activeByEntity = new ConcurrentHashMap<>();
    private BukkitTask task;
    private int patternCounter;

    public BossFightManager(JavaPlugin plugin, EconomyAPI economyAPI) {
        this.plugin = plugin;
        this.economyAPI = economyAPI;
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

        LivingEntity boss = (LivingEntity) altar.summonLocation().getWorld()
                .spawnEntity(altar.summonLocation(), EntityType.WITHER_SKELETON);
        double hp = BASE_HP * (1 + 0.5 * (partySize - 1));
        var maxHealthAttribute = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(hp);
            boss.setHealth(hp);
        }
        boss.setCustomName("§4§l공허의 기사");
        boss.setCustomNameVisible(true);
        boss.setGlowing(true);
        boss.getPersistentDataContainer().set(MobKeys.BOSS_MARKER, PersistentDataType.STRING, altar.getId());

        BossInstance instance = new BossInstance(altar.getId(), boss);
        activeByEntity.put(boss.getUniqueId(), instance);

        summoner.getWorld().strikeLightningEffect(altar.summonLocation());
        Bukkit.broadcastMessage("§4§l[보스 출현] §c공허의 기사가 제단에서 깨어났습니다! §7(권장 인원: " + altar.getRecommendedPartySize() + "명)");
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
        for (UUID participant : instance.getParticipants()) {
            economyAPI.addClam(participant, 1000);
            economyAPI.addPearl(participant, 10);
            economyAPI.addRep(participant, 50);
            Player player = Bukkit.getPlayer(participant);
            if (player != null) {
                player.sendTitle("§a[보스 처치]", "§f공허의 기사를 쓰러뜨렸습니다!", 10, 70, 20);
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
                Bukkit.broadcastMessage("§4공허의 기사가 2페이즈에 돌입합니다!");
            }

            if (instance.getElapsedSeconds() > ENRAGE_SECONDS && !instance.isEnraged()) {
                instance.setEnraged(true);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                Bukkit.broadcastMessage("§4§l[격노] §c공허의 기사가 격노했습니다!");
            }

            if (patternCounter % 5 == 0) {
                if (instance.getPhase() == 1) {
                    pullPattern(entity);
                } else {
                    blockExplosionPattern(entity);
                }
            }
        }
    }

    private void pullPattern(LivingEntity boss) {
        for (var nearby : boss.getWorld().getNearbyEntities(boss.getLocation(), 15, 15, 15)) {
            if (nearby instanceof Player player) {
                Vector pull = boss.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.6);
                player.setVelocity(pull);
            }
        }
        boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SHOOT, 1f, 0.6f);
    }

    private void blockExplosionPattern(LivingEntity boss) {
        for (var nearby : boss.getWorld().getNearbyEntities(boss.getLocation(), 20, 20, 20)) {
            if (nearby instanceof Player player) {
                player.getWorld().createExplosion(player.getLocation(), 0f, false, false);
                player.damage(4.0, boss);
            }
        }
        boss.playEffect(EntityEffect.HURT);
    }
}
