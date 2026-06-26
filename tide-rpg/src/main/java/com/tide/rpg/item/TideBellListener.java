package com.tide.rpg.item;

import com.tide.core.TideCorePlugin;
import com.tide.core.tide.TideState;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

public final class TideBellListener implements Listener {

    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("tide", "item_id");

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        String itemId = meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
        if (itemId == null || !itemId.equalsIgnoreCase("tide_bell")) {
            return;
        }

        event.setCancelled(true);

        // Consume 1 bell
        item.setAmount(item.getAmount() - 1);
        player.setItemInHand(item);

        // Trigger Blood Moon
        TideCorePlugin.getInstance().getTideScheduler().forceState(TideState.BLOOD_MOON);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.7f);
        player.sendMessage("§4§l[조종의 종] §c종소리가 울려 퍼지며 붉은 달이 차오르고 차원의 틈이 흔들립니다!");

        // Spawn a local wave of elite monsters
        int spawnCount = ThreadLocalRandom.current().nextInt(4, 7);
        for (int i = 0; i < spawnCount; i++) {
            double rx = ThreadLocalRandom.current().nextDouble(-8, 8);
            double rz = ThreadLocalRandom.current().nextDouble(-8, 8);
            org.bukkit.Location loc = player.getLocation().clone().add(rx, 0, rz);
            loc.setY(player.getWorld().getHighestBlockYAt(loc) + 1);

            EntityType type = ThreadLocalRandom.current().nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
            LivingEntity spawned = (LivingEntity) player.getWorld().spawnEntity(loc, type);
            spawned.setGlowing(true);
            
            // Mark as elite
            spawned.getPersistentDataContainer().set(new NamespacedKey("tide", "elite"), PersistentDataType.BYTE, (byte) 1);
            spawned.setCustomName("§c[블러드 침략자] §f" + type.name().toLowerCase());
            spawned.setCustomNameVisible(true);
        }
    }
}
