package com.tide.rpg.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/** Marks an open Inventory as the Tide shop so ShopListener can identify clicks reliably. */
public final class ShopHolder implements InventoryHolder {

    private final List<ShopEntry> buyEntries;
    private final List<ShopEntry> sellEntries;
    private ShopTab tab = ShopTab.BUY;
    private Inventory inventory;

    public ShopHolder(List<ShopEntry> buyEntries, List<ShopEntry> sellEntries) {
        this.buyEntries = buyEntries;
        this.sellEntries = sellEntries;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public ShopTab getTab() {
        return tab;
    }

    public void setTab(ShopTab tab) {
        this.tab = tab;
    }

    public List<ShopEntry> getBuyEntries() {
        return buyEntries;
    }

    public List<ShopEntry> getSellEntries() {
        return sellEntries;
    }

    public ShopEntry entryAt(int slot) {
        List<ShopEntry> currentList = (tab == ShopTab.BUY) ? buyEntries : sellEntries;
        return currentList.stream().filter(e -> e.slot() == slot).findFirst().orElse(null);
    }
}
