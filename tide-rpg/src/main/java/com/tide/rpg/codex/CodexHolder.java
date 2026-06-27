package com.tide.rpg.codex;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Inventory holder carrying tab and page state for the Codex GUI. */
public final class CodexHolder implements InventoryHolder {

    private final CodexTab tab;
    private final int page;
    private Inventory inventory;

    public CodexHolder(CodexTab tab, int page) {
        this.tab = tab;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public CodexTab getTab() { return tab; }
    public int getPage() { return page; }
}
