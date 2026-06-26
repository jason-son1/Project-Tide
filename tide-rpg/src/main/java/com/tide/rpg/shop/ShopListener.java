package com.tide.rpg.shop;

import com.tide.core.economy.EconomyAPI;
import com.tide.rpg.item.ItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class ShopListener implements Listener {

    private final EconomyAPI economyAPI;
    private final ItemFactory itemFactory;

    public ShopListener(EconomyAPI economyAPI, ItemFactory itemFactory) {
        this.economyAPI = economyAPI;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder holder)) {
            return;
        }
        event.setCancelled(true);

        ShopEntry entry = holder.entryAt(event.getRawSlot());
        if (entry == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        boolean charged = entry.currency() == ShopEntry.Currency.CLAM
                ? economyAPI.takeClam(player.getUniqueId(), entry.price())
                : economyAPI.takePearl(player.getUniqueId(), entry.price());

        if (!charged) {
            player.sendMessage("§c재화가 부족합니다.");
            return;
        }

        player.getInventory().addItem(itemFactory.create(entry.itemId()));
        player.sendMessage("§a구매 완료: §f" + entry.itemId());
    }
}
