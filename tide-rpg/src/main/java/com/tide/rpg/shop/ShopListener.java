package com.tide.rpg.shop;

import com.tide.core.economy.EconomyAPI;
import com.tide.rpg.item.ItemFactory;
import com.tide.rpg.rune.RuneItemFactory;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class ShopListener implements Listener {

    private final EconomyAPI economyAPI;
    private final ItemFactory itemFactory;
    private final RuneItemFactory runeItemFactory;
    private final ShopGUI shopGUI;

    public ShopListener(EconomyAPI economyAPI, ItemFactory itemFactory, RuneItemFactory runeItemFactory, ShopGUI shopGUI) {
        this.economyAPI = economyAPI;
        this.itemFactory = itemFactory;
        this.runeItemFactory = runeItemFactory;
        this.shopGUI = shopGUI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        // Handle tab switching
        if (slot == 0) {
            if (holder.getTab() != ShopTab.BUY) {
                holder.setTab(ShopTab.BUY);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
                shopGUI.render(event.getInventory(), ShopTab.BUY, player);
            }
            return;
        }

        if (slot == 1) {
            if (holder.getTab() != ShopTab.SELL) {
                holder.setTab(ShopTab.SELL);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
                shopGUI.render(event.getInventory(), ShopTab.SELL, player);
            }
            return;
        }

        if (slot == 3) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            shopGUI.changePage(event.getInventory(), holder, player, -1);
            return;
        }

        if (slot == 5) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            shopGUI.changePage(event.getInventory(), holder, player, 1);
            return;
        }

        if (slot == 8) {
            com.tide.core.TideCorePlugin corePlugin = com.tide.core.TideCorePlugin.getPlugin(com.tide.core.TideCorePlugin.class);
            new com.tide.core.guide.GuideGUI(corePlugin.getGuideRegistry()).openEntries(player, com.tide.core.guide.GuideCategory.DEATH);
            return;
        }

        if (slot < 9) {
            return;
        }

        ShopEntry entry = holder.entryAt(slot);
        if (entry == null) {
            return;
        }

        if (holder.getTab() == ShopTab.BUY) {
            boolean charged = entry.currency() == ShopEntry.Currency.CLAM
                    ? economyAPI.takeClam(player.getUniqueId(), entry.price())
                    : economyAPI.takePearl(player.getUniqueId(), entry.price());

            if (!charged) {
                player.sendMessage("§c재화가 부족합니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            ItemStack purchased = createItem(entry);
            player.getInventory().addItem(purchased);
            player.sendMessage("§a구매 완료: §f" + purchased.getItemMeta().getDisplayName());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        } else {
            int hasCount = shopGUI.countInventoryItems(player, entry);
            if (hasCount <= 0) {
                player.sendMessage("§c판매할 아이템을 가지고 있지 않습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            boolean sellAll = event.isRightClick();
            int amountToSell = sellAll ? hasCount : 1;

            removeInventoryItems(player, entry, amountToSell, shopGUI);

            long reward = entry.price() * amountToSell;
            if (entry.currency() == ShopEntry.Currency.CLAM) {
                economyAPI.addClam(player.getUniqueId(), reward);
                player.sendMessage("§a아이템 §f" + amountToSell + "개§a를 판매하여 §6조개 " + reward + "개§a를 획득했습니다.");
            } else {
                economyAPI.addPearl(player.getUniqueId(), reward);
                player.sendMessage("§a아이템 §f" + amountToSell + "개§a를 판매하여 §d진주 " + reward + "개§a를 획득했습니다.");
            }
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

            shopGUI.render(event.getInventory(), ShopTab.SELL, player);
        }
    }

    private ItemStack createItem(ShopEntry entry) {
        if (entry.kind() == ShopEntry.Kind.RUNE) {
            return runeItemFactory.create(entry.itemId());
        } else if (entry.kind() == ShopEntry.Kind.ITEM) {
            return itemFactory.create(entry.itemId());
        } else {
            Material mat = Material.matchMaterial(entry.itemId());
            if (mat == null) mat = Material.STONE;
            return new ItemStack(mat);
        }
    }

    private void removeInventoryItems(Player player, ShopEntry entry, int amount, ShopGUI shopGUI) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (shopGUI.matches(item, entry)) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                    player.getInventory().setItem(i, item);
                }
            }
            if (remaining <= 0) {
                break;
            }
        }
    }
}
