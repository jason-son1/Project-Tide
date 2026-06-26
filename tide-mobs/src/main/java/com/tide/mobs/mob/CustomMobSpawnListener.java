package com.tide.mobs.mob;

import com.tide.core.economy.EconomyAPI;
import com.tide.core.tide.TideStateProvider;
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

    public CustomMobSpawnListener(TideStateProvider stateProvider, MobRegistry mobRegistry,
                                  AffixRegistry affixRegistry, EliteProcessor eliteProcessor,
                                  ItemFactory itemFactory, EconomyAPI economyAPI) {
        this.stateProvider = stateProvider;
        this.mobRegistry = mobRegistry;
        this.affixRegistry = affixRegistry;
        this.eliteProcessor = eliteProcessor;
        this.itemFactory = itemFactory;
        this.economyAPI = economyAPI;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Handle natural and spawner spawns
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL &&
                event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }

        LivingEntity entity = event.getEntity();
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

        // Apply attributes (HP, Damage, Speed)
        if (mob.getHpMultiplier() != 1.0) {
            AttributeInstance hpAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hpAttr != null) {
                double newMax = hpAttr.getBaseValue() * mob.getHpMultiplier();
                hpAttr.setBaseValue(newMax);
                entity.setHealth(newMax);
            }
        }

        if (mob.getDamageMultiplier() != 1.0) {
            AttributeInstance dmgAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (dmgAttr != null) {
                dmgAttr.setBaseValue(dmgAttr.getBaseValue() * mob.getDamageMultiplier());
            }
        }

        if (mob.getMovementSpeed() > 0) {
            AttributeInstance speedAttr = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(mob.getMovementSpeed());
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
