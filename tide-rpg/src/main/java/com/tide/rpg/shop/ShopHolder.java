package com.tide.rpg.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/** Marks an open Inventory as the Tide shop so ShopListener can identify clicks reliably. */
public final class ShopHolder implements InventoryHolder {

    private final List<ShopEntry> entries;
    private Inventory inventory;

    public ShopHolder(List<ShopEntry> entries) {
        this.entries = entries;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public ShopEntry entryAt(int slot) {
        return entries.stream().filter(e -> e.slot() == slot).findFirst().orElse(null);
    }
}
