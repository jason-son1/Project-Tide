package com.tide.rpg.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Marks an open Inventory as the Tide shop so ShopListener can identify
 * clicks reliably. Tracks the current page per tab and a slot→entry map
 * rebuilt on every render() call, since item position is now computed
 * dynamically (paginated) rather than pinned to a fixed slot number.
 */
public final class ShopHolder implements InventoryHolder {

    private final List<ShopEntry> buyEntries;
    private final List<ShopEntry> sellEntries;
    private final Map<ShopTab, Integer> pageByTab = new HashMap<>();
    private final Map<ShopTab, Map<Integer, ShopEntry>> slotMapByTab = new HashMap<>();
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

    public int getPage(ShopTab tab) {
        return pageByTab.getOrDefault(tab, 0);
    }

    public void setPage(ShopTab tab, int page) {
        pageByTab.put(tab, page);
    }

    public void setSlotMap(ShopTab tab, Map<Integer, ShopEntry> slotMap) {
        slotMapByTab.put(tab, slotMap);
    }

    public ShopEntry entryAt(int slot) {
        Map<Integer, ShopEntry> slotMap = slotMapByTab.get(tab);
        return slotMap == null ? null : slotMap.get(slot);
    }
}
