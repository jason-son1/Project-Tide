package com.tide.core.guide;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class GuideListener implements Listener {

    private final GuideGUI guideGUI;

    public GuideListener(GuideGUI guideGUI) {
        this.guideGUI = guideGUI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof GuideGUI.CategoryHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int index = event.getRawSlot() - 10;
            if (index < 0 || index >= holder.getCategories().size()) {
                return;
            }
            guideGUI.openEntries(player, holder.getCategories().get(index));
            return;
        }

        if (event.getInventory().getHolder() instanceof GuideGUI.EntryHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            int slot = event.getRawSlot();
            if (slot == 45) {
                guideGUI.openCategories(player);
                return;
            }
            int index = slot - 9;
            if (index < 0 || index >= holder.getEntries().size()) {
                return;
            }
            guideGUI.openBook(player, holder.getEntries().get(index));
        }
    }
}
