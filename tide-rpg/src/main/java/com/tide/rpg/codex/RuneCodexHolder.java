package com.tide.rpg.codex;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Inventory holder marking the Rune Codex GUI (no tab/page state needed — every rune fits on one screen). */
public final class RuneCodexHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
