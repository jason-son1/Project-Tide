package com.tide.mobs.mob;

import com.tide.core.difficulty.DifficultyManager;
import com.tide.core.difficulty.DifficultyResult;
import com.tide.core.economy.EconomyAPI;
import com.tide.core.tide.TideStateProvider;
import com.tide.mobs.difficulty.WorldDifficultyApplier;
import com.tide.mobs.affix.EliteProcessor;
import com.tide.mobs.affix.AffixRegistry;
import com.tide.mobs.affix.AffixDefinition;
import com.tide.mobs.MobKeys;
import com.tide.rpg.item.ItemFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class CustomMobSpawnListener implements Listener {

    private static final NamespacedKey CUSTOM_MOB_KEY = new NamespacedKey("tide", "custom_mob_id");
    private static final NamespacedKey CUSTOM_MODEL_KEY = new NamespacedKey("tide", "cmd");

    private final TideStateProvider stateProvider;
    private final MobRegistry mobRegistry;
    private final AffixRegistry affixRegistry;
    private final EliteProcessor eliteProcessor;
    private final ItemFactory itemFactory;
    private final EconomyAPI economyAPI;
    private final DifficultyManager difficultyManager;

    public CustomMobSpawnListener(TideStateProvider stateProvider, MobRegistry mobRegistry,
                                  AffixRegistry affixRegistry, EliteProcessor eliteProcessor,
                                  ItemFactory itemFactory, EconomyAPI economyAPI, DifficultyManager difficultyManager) {
        this.stateProvider = stateProvider;
        this.mobRegistry = mobRegistry;
        this.affixRegistry = affixRegistry;
        this.eliteProcessor = eliteProcessor;
        this.itemFactory = itemFactory;
        this.economyAPI = economyAPI;
        this.difficultyManager = difficultyManager;
    }

    private static final NamespacedKey FORCE_CUSTOM_MOB_KEY = new NamespacedKey("tide", "force_custom_mob_id");

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();

        // Check if there is a forced custom mob ID in PDC
        String forcedId = null;
        if (entity.getPersistentDataContainer().has(FORCE_CUSTOM_MOB_KEY, PersistentDataType.STRING)) {
            forcedId = entity.getPersistentDataContainer().get(FORCE_CUSTOM_MOB_KEY, PersistentDataType.STRING);
        }

        if (forcedId != null) {
            CustomMob mob = mobRegistry.get(forcedId);
            if (mob != null) {
                transform(entity, mob);
            }
            return;
        }

        // Handle natural and spawner spawns
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL &&
                event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }

        String baseTypeName = entity.getType().name();

        List<CustomMob> candidates = new ArrayList<>();
        int totalWeight = 0;

        String world = entity.getWorld().getName();
        String biome = entity.getLocation().getBlock().getBiome().name();
        String tide = stateProvider.getCurrentState().name();

        for (CustomMob mob : mobRegistry.all()) {
            if (mob.getBaseMob().equalsIgnoreCase(baseTypeName)) {
                // Filter by world, biome, and tide state
                boolean worldOk = mob.getSpawnWorlds().isEmpty() || mob.getSpawnWorlds().contains(world);
                boolean biomeOk = mob.getSpawnBiomes().isEmpty() || mob.getSpawnBiomes().contains(biome);
                boolean tideOk = mob.getSpawnTideStates().isEmpty() || mob.getSpawnTideStates().contains(tide);

                if (worldOk && biomeOk && tideOk) {
                    candidates.add(mob);
                    totalWeight += mob.getSpawnWeight();
                }
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        // Roll spawn chance: 15% base rate for converting to a custom mob
        double spawnRoll = ThreadLocalRandom.current().nextDouble();
        if (spawnRoll > 0.15) {
            return;
        }

        // Select custom mob proportionally by weight
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;
        CustomMob selected = null;
        for (CustomMob mob : candidates) {
            currentWeight += mob.getSpawnWeight();
            if (roll < currentWeight) {
                selected = mob;
                break;
            }
        }

        if (selected != null) {
            transform(entity, selected);
        }
    }

    private void transform(LivingEntity entity, CustomMob mob) {
        // Tag custom mob ID and custom model data in PDC
        entity.getPersistentDataContainer().set(CUSTOM_MOB_KEY, PersistentDataType.STRING, mob.getId());
        if (mob.getCustomModelData() > 0) {
            entity.getPersistentDataContainer().set(CUSTOM_MODEL_KEY, PersistentDataType.INTEGER, mob.getCustomModelData());
            
            // Set CMD on helmet for humanoids (Zombie, Skeleton, etc.) if they can wear armor
            if (entity.getEquipment() != null) {
                ItemStack helmet = entity.getEquipment().getHelmet();
                if (helmet == null || helmet.getType().isAir()) {
                    org.bukkit.Material mat = entity.getType() == org.bukkit.entity.EntityType.SKELETON 
                        ? org.bukkit.Material.SKELETON_SKULL : org.bukkit.Material.ZOMBIE_HEAD;
                    ItemStack customHelmet = new ItemStack(mat);
                    ItemMeta meta = customHelmet.getItemMeta();
                    if (meta != null) {
                        meta.setCustomModelData(mob.getCustomModelData());
                        customHelmet.setItemMeta(meta);
                    }
                    entity.getEquipment().setHelmet(customHelmet);
                }
            }
        }

        // Apply display name
        entity.setCustomName(mob.getDisplayName());
        entity.setCustomNameVisible(true);

        // Apply the mob's own authored multipliers first — these become the "unscaled
        // base" that WorldDifficultyApplier multiplies on top of and can later refresh
        // without compounding.
        AttributeInstance hpAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hpAttr != null && mob.getHpMultiplier() != 1.0) {
            double newMax = hpAttr.getBaseValue() * mob.getHpMultiplier();
            hpAttr.setBaseValue(newMax);
            entity.setHealth(newMax);
        }

        AttributeInstance dmgAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmgAttr != null && mob.getDamageMultiplier() != 1.0) {
            dmgAttr.setBaseValue(dmgAttr.getBaseValue() * mob.getDamageMultiplier());
        }

        // World-difficulty multiplier (Dynamic World Difficulty Scaling) — applied on
        // top, tracked in PDC so the periodic refresh task can keep this mob in sync
        // with the world's current difficulty without re-stacking the multiplier.
        if (difficultyManager != null && difficultyManager.isEnabled()) {
            DifficultyResult difficulty = difficultyManager.resolve(entity.getLocation());
            WorldDifficultyApplier.apply(entity, difficulty);
        }

        if (mob.getMovementSpeed() > 0) {
            AttributeInstance speedAttr = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                // +35% cap over vanilla default so scaled mobs don't feel like they're teleporting.
                double cap = speedAttr.getDefaultValue() * 1.35;
                speedAttr.setBaseValue(Math.min(mob.getMovementSpeed(), cap));
            }
        }

        // Apply affixes using EliteProcessor
        if (!mob.getAffixes().isEmpty()) {
            // Re-use Elite tagging
            entity.getPersistentDataContainer().set(MobKeys.ELITE, PersistentDataType.BYTE, (byte) 1);
            entity.getPersistentDataContainer().set(MobKeys.AFFIXES, PersistentDataType.STRING, String.join(",", mob.getAffixes()));
            List<AffixDefinition> affixDefs = new ArrayList<>();
            for (String affixId : mob.getAffixes()) {
                AffixDefinition def = affixRegistry.get(affixId);
                if (def != null) {
                    affixDefs.add(def);
                }
            }
            if (!affixDefs.isEmpty()) {
                eliteProcessor.apply(entity, affixDefs);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String mobId = entity.getPersistentDataContainer().get(CUSTOM_MOB_KEY, PersistentDataType.STRING);
        if (mobId == null) {
            return;
        }

        CustomMob mob = mobRegistry.get(mobId);
        if (mob == null) {
            return;
        }

        // Clear default drops
        event.getDrops().clear();

        // 조개/진주 드롭 배율: WDM의 50%만큼 추가 수량 (Dynamic World Difficulty Scaling spec 5장)
        double dropBonus = 1.0;
        if (difficultyManager != null && difficultyManager.isEnabled()) {
            dropBonus = 1.0 + 0.5 * (difficultyManager.resolve(entity.getLocation()).hpMultiplier() - 1.0);
        }

        // Roll and drop custom loot
        Player killer = entity.getKiller();
        for (Map<String, Object> drop : mob.getDrops()) {
            double chance = ((Number) drop.getOrDefault("chance", 1.0)).doubleValue();
            if (ThreadLocalRandom.current().nextDouble() > chance) {
                continue;
            }

            String itemId = (String) drop.get("item_id");
            String currency = (String) drop.get("currency");

            if (itemId != null && itemFactory != null) {
                ItemStack item = itemFactory.create(itemId);
                if (item != null) {
                    event.getDrops().add(item);
                }
            } else if (currency != null && killer != null && economyAPI != null) {
                Object amountObj = drop.get("amount");
                long amount = 0;
                if (amountObj instanceof List) {
                    List<?> range = (List<?>) amountObj;
                    if (range.size() >= 2) {
                        int min = ((Number) range.get(0)).intValue();
                        int max = ((Number) range.get(1)).intValue();
                        amount = ThreadLocalRandom.current().nextInt(min, max + 1);
                    }
                } else if (amountObj instanceof Number) {
                    amount = ((Number) amountObj).longValue();
                } else {
                    amount = 1;
                }

                if (amount > 0) {
                    amount = Math.round(amount * dropBonus);
                    if ("clam".equalsIgnoreCase(currency)) {
                        economyAPI.addClam(killer.getUniqueId(), amount);
                        killer.sendMessage("§6+" + amount + " 조개 §7(커스텀 몹 처치 보상)");
                    } else if ("pearl".equalsIgnoreCase(currency)) {
                        economyAPI.addPearl(killer.getUniqueId(), amount);
                        killer.sendMessage("§d+" + amount + " 진주 §7(커스텀 몹 처치 보상)");
                    }
                }
            }
        }
    }
}
