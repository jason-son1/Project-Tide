package com.tide.rpg.forge;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ForgeHolder implements InventoryHolder {

    private Inventory inventory;
    private ForgeTab tab = ForgeTab.REINFORCE;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public ForgeTab getTab() {
        return tab;
    }

    public void setTab(ForgeTab tab) {
        this.tab = tab;
    }
}
