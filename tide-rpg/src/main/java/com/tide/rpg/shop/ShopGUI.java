package com.tide.rpg.shop;

import com.tide.rpg.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Hardcoded prototype shop (stage 1 scope). Item IDs/prices will move to a
 * config-driven schema once the 2파트 Config Layer lands.
 */
public final class ShopGUI {

    public static final List<ShopEntry> ENTRIES = List.of(
            new ShopEntry("iron_sword_t1", 200, ShopEntry.Currency.CLAM, 10),
            new ShopEntry("flame_sword_t1", 350, ShopEntry.Currency.CLAM, 12),
            new ShopEntry("leather_armor_t1", 150, ShopEntry.Currency.CLAM, 14),
            new ShopEntry("reinforce_stone", 50, ShopEntry.Currency.CLAM, 16),
            new ShopEntry("protection_scroll", 3, ShopEntry.Currency.PEARL, 22)
    );

    private final ItemFactory itemFactory;

    public ShopGUI(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    public void open(Player player) {
        ShopHolder holder = new ShopHolder(ENTRIES);
        Inventory inventory = Bukkit.createInventory(holder, 27, "§6The Tide 상점");
        holder.setInventory(inventory);

        for (ShopEntry entry : ENTRIES) {
            ItemStack display = itemFactory.create(entry.itemId());
            ItemMeta meta = display.getItemMeta();
            List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
            lore.add("");
            String priceLabel = entry.currency() == ShopEntry.Currency.CLAM
                    ? "§6조개 " + entry.price() + "개"
                    : "§d진주 " + entry.price() + "개";
            lore.add("§a구매가: " + priceLabel);
            lore.add("§e클릭하여 구매");
            meta.setLore(lore);
            display.setItemMeta(meta);
            inventory.setItem(entry.slot(), display);
        }

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }

        player.openInventory(inventory);
    }
}
