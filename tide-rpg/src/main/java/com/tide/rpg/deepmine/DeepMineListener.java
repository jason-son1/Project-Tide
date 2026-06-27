package com.tide.rpg.deepmine;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class DeepMineListener implements Listener {

    public static final String DEEPMINE_DEATH_METADATA = "tide_deepmine_death";

    private final DeepMineManager manager;
    private final Plugin plugin;

    public DeepMineListener(DeepMineManager manager, Plugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!manager.isInside(player.getLocation())) {
            return;
        }
        manager.trackLoot(player, event.getItem().getItemStack());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!manager.isInside(player.getLocation())) {
            return;
        }
        player.setMetadata(DEEPMINE_DEATH_METADATA, new FixedMetadataValue(plugin, true));
        manager.onDeathInside(player);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        var block = event.getClickedBlock();
        Player player = event.getPlayer();

        // 1. Portal entrance right-click
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && manager.getActivePortals().containsKey(block)) {
            event.setCancelled(true);
            manager.enter(player);
            return;
        }

        // 2. Deep Mine chest opening trap
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && manager.isInside(block.getLocation())) {
            if (block.getType() == Material.CHEST || block.getType() == Material.BARREL || block.getType() == Material.TRAPPED_CHEST || block.getType() == Material.ENDER_CHEST) {
                
                if (manager.isTrapChest(block.getLocation()) && !block.hasMetadata("tide_trap_triggered")) {
                    event.setCancelled(true);
                    block.setMetadata("tide_trap_triggered", new FixedMetadataValue(plugin, true));
                    block.setType(Material.AIR);
                    player.sendMessage("§c§l[!] 쾅! 열려던 상자가 가짜 폭탄 상자였습니다!");
                    Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);
                    loc.getWorld().createExplosion(loc, 4.0f, false, false);
                    for (int i = 0; i < 4; i++) {
                        Location spawnLoc = loc.clone().add(
                            ThreadLocalRandom.current().nextDouble(-2, 2),
                            0,
                            ThreadLocalRandom.current().nextDouble(-2, 2)
                        );
                        spawnForcedCustomMob(spawnLoc, "brine_spider", player);
                    }
                    return;
                }

                if (!block.hasMetadata("tide_chest_opened")) {
                    block.setMetadata("tide_chest_opened", new FixedMetadataValue(plugin, true));
                    double r = ThreadLocalRandom.current().nextDouble();
                    if (r < 0.20) {
                        player.sendMessage("§c§l[!] 상자를 열자마자 어둠 속에서 몬스터들이 급습했습니다!");
                        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
                        for (int i = 0; i < 3; i++) {
                            Location spawnLoc = block.getLocation().clone().add(
                                ThreadLocalRandom.current().nextDouble(-3, 3),
                                0,
                                ThreadLocalRandom.current().nextDouble(-3, 3)
                            );
                            String mobId = ThreadLocalRandom.current().nextBoolean() ? "mine_saboteur_creeper" : "abyssal_husk";
                            spawnForcedCustomMob(spawnLoc, mobId, player);
                        }
                    } else if (r < 0.30) {
                        player.sendMessage("§c§l[!] 치익- 상자에서 독성 유독가스가 누출되었습니다!");
                        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 1));
                        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.8f);
                        player.spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 40, 1.0, 1.0, 1.0, 0.05, 1.0f);
                    }
                }
            }
        }

        // 3. Deep Mine pressure plate trap
        if (event.getAction() == Action.PHYSICAL && manager.isInside(block.getLocation())) {
            if (block.getType() == Material.STONE_PRESSURE_PLATE || block.getType() == Material.OAK_PRESSURE_PLATE) {
                if (!block.hasMetadata("tide_plate_triggered")) {
                    block.setMetadata("tide_plate_triggered", new FixedMetadataValue(plugin, true));
                    double r = ThreadLocalRandom.current().nextDouble();
                    if (r < 0.40) {
                        Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);
                        loc.getWorld().createExplosion(loc, 3.0f, false, false);
                        player.sendMessage("§c§l[위험] 쾅! 압력판을 밟아 폭발성 덫이 발동되었습니다!");
                    } else if (r < 0.70) {
                        player.sendMessage("§c§l[위험] 압력판을 밟자 천장이 무너져 내립니다!");
                        player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 0.5f);
                        Location roof = block.getLocation().clone().add(0, 4, 0);
                        roof.getBlock().setType(Material.GRAVEL);
                        player.damage(4.0);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location blockLoc = event.getBlock().getLocation();

        if (!manager.isInside(player.getLocation())) {
            // Check for discovery in the configured deepmine world, Y <= 30
            if (player.getWorld().getName().equals(blockLoc.getWorld().getName()) && blockLoc.getBlockY() <= 30) {
                Material type = event.getBlock().getType();
                if (type == Material.STONE || type == Material.DEEPSLATE || type.name().endsWith("_ORE")) {
                    double chance = plugin.getConfig().getDouble("deepmine.discovery-chance", 0.002);
                    if (ThreadLocalRandom.current().nextDouble() < chance) {
                        manager.spawnPortalEntrance(blockLoc);
                    }
                }
            }
            return;
        }

        // They are inside the Deep Mine!
        // Apply deepmine_pickaxe reward check
        Material blockType = event.getBlock().getType();
        if (blockType.name().endsWith("_ORE") || blockType == Material.RAW_IRON_BLOCK || blockType == Material.RAW_GOLD_BLOCK || blockType == Material.RAW_COPPER_BLOCK) {
            org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.hasItemMeta()) {
                var pdc = hand.getItemMeta().getPersistentDataContainer();
                String itemId = pdc.get(new NamespacedKey("tide", "item_id"), PersistentDataType.STRING);
                if ("deepmine_pickaxe".equals(itemId)) {
                    // Extra drop!
                    Location dropLoc = blockLoc.clone().add(0.5, 0.5, 0.5);
                    blockLoc.getWorld().dropItemNaturally(dropLoc, new org.bukkit.inventory.ItemStack(getRawMaterial(blockType), 1));
                    player.sendMessage("§6[심연 곡괭이] §7광석이 추가로 1개 더 드롭되었습니다!");
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
                }
            }
        }

        // Apply deepmine_lantern check for night vision
        checkLanternNightVision(player);

        double hazardChance = plugin.getConfig().getDouble("deepmine.hazard-chance", 0.05);
        if (ThreadLocalRandom.current().nextDouble() < hazardChance) {
            triggerMiningHazard(player, blockLoc.add(0.5, 0.5, 0.5));
        }
    }

    private void checkLanternNightVision(Player player) {
        boolean hasLantern = false;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                var pdc = item.getItemMeta().getPersistentDataContainer();
                String itemId = pdc.get(new NamespacedKey("tide", "item_id"), PersistentDataType.STRING);
                if ("deepmine_lantern".equals(itemId)) {
                    hasLantern = true;
                    break;
                }
            }
        }
        if (hasLantern) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false, true));
        }
    }

    private Material getRawMaterial(Material ore) {
        return switch (ore) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Material.COAL;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.RAW_IRON;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.RAW_GOLD;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Material.EMERALD;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER;
            default -> Material.COAL;
        };
    }


    private void triggerMiningHazard(Player player, Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() < 0.7) {
            // Spawn a monster
            EntityType[] mobTypes = {EntityType.ZOMBIE, EntityType.CAVE_SPIDER, EntityType.WITHER_SKELETON};
            EntityType choice = mobTypes[ThreadLocalRandom.current().nextInt(mobTypes.length)];
            Mob mob = (Mob) world.spawnEntity(loc, choice);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100000, 1));
            mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100000, 1));
            mob.setCustomName("§c§l심해 광산 괴물");
            mob.setCustomNameVisible(true);
            mob.setTarget(player);

            world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.5f);
            world.spawnParticle(Particle.LARGE_SMOKE, loc, 30, 0.5, 0.5, 0.5, 0.05);
            player.sendMessage("§c[!] 광산 깊은 곳의 어둠 속에서 괴물이 깨어났습니다!");
        } else {
            // Cave-in / explosion
            world.createExplosion(loc, 1.5f, false, false);
            player.sendMessage("§c[위험] 광산이 붕괴하며 가스가 폭발했습니다!");
        }
    }

    @EventHandler
    public void onSpawnerSpawn(org.bukkit.event.entity.SpawnerSpawnEvent event) {
        if (event.getSpawner() != null) {
            Location loc = event.getSpawner().getLocation();
            String mobId = manager.getSpawnerMobId(loc);
            if (mobId != null) {
                event.getEntity().getPersistentDataContainer().set(
                    new NamespacedKey("tide", "force_custom_mob_id"),
                    PersistentDataType.STRING,
                    mobId
                );
            }
        }
    }

    private void spawnForcedCustomMob(Location loc, String mobId, Player target) {
        World world = loc.getWorld();
        if (world == null) return;

        if (mobId.equals("mine_saboteur_creeper")) {
            world.spawn(loc, org.bukkit.entity.Creeper.class, creeper -> {
                creeper.getPersistentDataContainer().set(new NamespacedKey("tide", "force_custom_mob_id"), PersistentDataType.STRING, mobId);
                creeper.setTarget(target);
            });
        } else if (mobId.equals("rustfang_skeleton")) {
            world.spawn(loc, org.bukkit.entity.Skeleton.class, skeleton -> {
                skeleton.getPersistentDataContainer().set(new NamespacedKey("tide", "force_custom_mob_id"), PersistentDataType.STRING, mobId);
                skeleton.setTarget(target);
            });
        } else if (mobId.equals("abyss_phantom")) {
            world.spawn(loc, org.bukkit.entity.Phantom.class, phantom -> {
                phantom.getPersistentDataContainer().set(new NamespacedKey("tide", "force_custom_mob_id"), PersistentDataType.STRING, mobId);
                phantom.setTarget(target);
            });
        } else if (mobId.equals("abyssal_guardian")) {
            world.spawn(loc, org.bukkit.entity.ElderGuardian.class, guardian -> {
                guardian.getPersistentDataContainer().set(new NamespacedKey("tide", "force_custom_mob_id"), PersistentDataType.STRING, mobId);
                guardian.setTarget(target);
            });
        } else if (mobId.equals("brine_spider")) {
            world.spawn(loc, org.bukkit.entity.CaveSpider.class, spider -> {
                spider.getPersistentDataContainer().set(new NamespacedKey("tide", "force_custom_mob_id"), PersistentDataType.STRING, mobId);
                spider.setTarget(target);
            });
        } else {
            world.spawn(loc, org.bukkit.entity.Zombie.class, zombie -> {
                zombie.getPersistentDataContainer().set(new NamespacedKey("tide", "force_custom_mob_id"), PersistentDataType.STRING, mobId);
                zombie.setTarget(target);
            });
        }
    }
}
