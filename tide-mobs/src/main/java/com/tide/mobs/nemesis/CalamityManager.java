package com.tide.mobs.nemesis;

import com.tide.mobs.affix.AffixDefinition;
import com.tide.mobs.affix.AffixRegistry;
import com.tide.rpg.TideKeys;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nemesis Mutation into "Calamity" (3-1): once a nemesis racks up enough kills
 * against its target without being put down, it stops being a personal rivalry
 * and becomes a server-wide threat — bigger, more dangerous, escorted, and
 * willing to raid whoever is nearby instead of just its original target.
 */
public final class CalamityManager {

    public static final int CALAMITY_KILL_THRESHOLD = 5;
    private static final int MAX_AFFIX_SLOTS = 4;
    private static final double STAT_MULTIPLIER = 4.0; // "+300%"

    private final AffixRegistry affixRegistry;
    private final Set<UUID> evolved = ConcurrentHashMap.newKeySet();

    public CalamityManager(AffixRegistry affixRegistry) {
        this.affixRegistry = affixRegistry;
    }

    public boolean isCalamityEligible(NemesisRecord record) {
        return record.getKillCount() >= CALAMITY_KILL_THRESHOLD;
    }

    public boolean hasEvolved(UUID mobUuid) {
        return evolved.contains(mobUuid);
    }

    public void evolve(LivingEntity mob, NemesisRecord record) {
        if (!evolved.add(mob.getUniqueId())) {
            return;
        }

        addExtraAffixes(mob);

        AttributeInstance maxHealthAttribute = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double newMax = maxHealthAttribute.getBaseValue() * STAT_MULTIPLIER;
            maxHealthAttribute.setBaseValue(newMax);
            mob.setHealth(newMax);
        }
        AttributeInstance damageAttribute = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.setBaseValue(damageAttribute.getBaseValue() * STAT_MULTIPLIER);
        }

        mob.setCustomName("§4§l[재앙] §c" + mob.getCustomName());
        var glowRangeManager = Bukkit.getServicesManager().load(com.tide.core.glow.GlowRangeManager.class);
        if (glowRangeManager != null) {
            glowRangeManager.register(mob, 24.0);
        } else {
            mob.setGlowing(true);
        }
        mob.getPersistentDataContainer().set(com.tide.mobs.MobKeys.CALAMITY, PersistentDataType.BYTE, (byte) 1);

        spawnZealotEscort(mob);

        mob.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, mob.getLocation(), 80, 2, 2, 2);
        Bukkit.broadcastMessage("§4§l[!] 재앙 등급 네메시스가 출현했습니다: §c" + record.getOriginalName()
                + " §7(목표: " + Bukkit.getOfflinePlayer(record.getPlayerUuid()).getName() + ")");
    }

    private void addExtraAffixes(LivingEntity mob) {
        String existingCsv = mob.getPersistentDataContainer().get(TideKeys.AFFIXES, PersistentDataType.STRING);
        Set<String> current = new LinkedHashSet<>();
        if (existingCsv != null && !existingCsv.isBlank()) {
            current.addAll(Arrays.asList(existingCsv.split(",")));
        }

        List<AffixDefinition> pool = new ArrayList<>(affixRegistry.all());
        java.util.Collections.shuffle(pool);

        AttributeInstance maxHealthAttribute = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance damageAttribute = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);

        for (AffixDefinition affix : pool) {
            if (current.size() >= MAX_AFFIX_SLOTS) {
                break;
            }
            if (current.contains(affix.getId())) {
                continue;
            }
            current.add(affix.getId());
            if (maxHealthAttribute != null) {
                maxHealthAttribute.setBaseValue(maxHealthAttribute.getBaseValue() * affix.getHpMultiplier());
            }
            if (damageAttribute != null) {
                damageAttribute.setBaseValue(damageAttribute.getBaseValue() * affix.getDamageMultiplier());
            }
        }

        mob.getPersistentDataContainer().set(TideKeys.AFFIXES, PersistentDataType.STRING, String.join(",", current));
        if (maxHealthAttribute != null) {
            mob.setHealth(maxHealthAttribute.getBaseValue());
        }
    }

    private void spawnZealotEscort(LivingEntity mob) {
        EntityType[] escortTypes = {EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON};
        int count = ThreadLocalRandom.current().nextInt(2, 4);
        for (int i = 0; i < count; i++) {
            EntityType type = escortTypes[ThreadLocalRandom.current().nextInt(escortTypes.length)];
            var escortLocation = mob.getLocation().add(
                    ThreadLocalRandom.current().nextInt(-3, 4), 0, ThreadLocalRandom.current().nextInt(-3, 4));
            var escort = mob.getWorld().spawnEntity(escortLocation, type);
            if (escort instanceof LivingEntity escortEntity) {
                escortEntity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
                escortEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
                escortEntity.setCustomName("§c[맹신] §f" + type.name().toLowerCase());
                escortEntity.setCustomNameVisible(true);
                escortEntity.getPersistentDataContainer().set(
                        com.tide.mobs.MobKeys.CALAMITY_ESCORT, PersistentDataType.STRING, mob.getUniqueId().toString());
            }
        }
        mob.playEffect(EntityEffect.WOLF_HEARTS);
    }
}
