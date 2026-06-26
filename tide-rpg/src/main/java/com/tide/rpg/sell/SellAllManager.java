package com.tide.rpg.sell;

import com.tide.core.economy.EconomyAPI;
import com.tide.rpg.TideKeys;
import com.tide.rpg.item.ItemDefinition;
import com.tide.rpg.item.ItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Geared items (anything carrying a "gs" PDC value) are always excluded —
 * the whole point of /sellall is to clear junk without risking your equipment.
 */
public final class SellAllManager {

    private final ItemRegistry itemRegistry;
    private final EconomyAPI economyAPI;

    public SellAllManager(ItemRegistry itemRegistry, EconomyAPI economyAPI) {
        this.itemRegistry = itemRegistry;
        this.economyAPI = economyAPI;
    }

    public record Preview(Map<String, Integer> countsByLabel, long totalClam) {
    }

    public Preview preview(Player player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        long total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            Long price = priceOf(item);
            if (price == null || price <= 0) {
                continue;
            }
            total += price * item.getAmount();
            counts.merge(labelOf(item), item.getAmount(), Integer::sum);
        }
        return new Preview(counts, total);
    }

    public Preview confirm(Player player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        long total = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            Long price = priceOf(item);
            if (price == null || price <= 0) {
                continue;
            }
            total += price * item.getAmount();
            counts.merge(labelOf(item), item.getAmount(), Integer::sum);
            contents[i] = null;
        }
        player.getInventory().setStorageContents(contents);
        if (total > 0) {
            economyAPI.addClam(player.getUniqueId(), total);
        }
        return new Preview(counts, total);
    }

    private Long priceOf(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Integer vanilla = VanillaPriceTable.priceOf(item.getType());
            return vanilla == null ? null : (long) vanilla;
        }

        var pdc = meta.getPersistentDataContainer();
        boolean isGear = pdc.has(TideKeys.GS, PersistentDataType.INTEGER)
                && pdc.getOrDefault(TideKeys.GS, PersistentDataType.INTEGER, 0) > 0;
        if (isGear) {
            return null;
        }

        String itemId = pdc.get(TideKeys.ITEM_ID, PersistentDataType.STRING);
        if (itemId != null) {
            ItemDefinition definition = itemRegistry.get(itemId);
            if (definition == null || definition.getSellPrice() <= 0) {
                return null;
            }
            return (long) definition.getSellPrice();
        }

        Integer vanilla = VanillaPriceTable.priceOf(item.getType());
        return vanilla == null ? null : (long) vanilla;
    }

    private String labelOf(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().name();
    }
}
