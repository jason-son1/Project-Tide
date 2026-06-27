package com.tide.rpg.item;

import com.tide.rpg.TideKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class FunctionalItemListener implements Listener {

    private final JavaPlugin plugin;

    public FunctionalItemListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        var pdc = item.getItemMeta().getPersistentDataContainer();
        String itemId = pdc.get(TideKeys.ITEM_ID, PersistentDataType.STRING);
        if (itemId == null) {
            return;
        }

        if ("spawn_setter".equals(itemId)) {
            event.setCancelled(true);
            if (!plugin.getConfig().getBoolean("functional-items.spawn_setter.enabled", true)) {
                player.sendMessage("§c이 아이템 기능은 비활성화되어 있습니다.");
                return;
            }

            player.setRespawnLocation(player.getLocation(), true);
            player.sendMessage("§a[영혼의 닻] §f현재 위치가 새로운 스폰 포인트로 지정되었습니다!");
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            consumeOne(player, item);
        } else if ("scroll_of_return".equals(itemId)) {
            event.setCancelled(true);
            if (!plugin.getConfig().getBoolean("functional-items.scroll_of_return.enabled", true)) {
                player.sendMessage("§c이 아이템 기능은 비활성화되어 있습니다.");
                return;
            }

            Location spawn = player.getRespawnLocation();
            if (spawn == null) {
                spawn = player.getWorld().getSpawnLocation();
            }

            player.sendMessage("§b[귀환 주문서] §f주문서를 활성화하여 스폰 위치로 순간이동합니다...");
            player.playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
            player.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);

            player.teleport(spawn);

            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
            consumeOne(player, item);
        }
    }

    private void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        }
    }
}
