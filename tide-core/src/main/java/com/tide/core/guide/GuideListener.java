package com.tide.core.guide;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
        if (!(event.getInventory().getHolder() instanceof GuideGUI.GuideHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == GuideGUI.CODEX_TAB_SLOT) {
            CodexOpener codexOpener = Bukkit.getServicesManager().load(CodexOpener.class);
            if (codexOpener != null) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                codexOpener.open(player);
            } else {
                player.sendMessage("§c아이템 도감을 사용할 수 없습니다.");
            }
            return;
        }

        GuideCategory[] categories = GuideCategory.values();
        for (int i = 0; i < categories.length && i < GuideGUI.CATEGORY_TAB_SLOTS.length; i++) {
            if (slot == GuideGUI.CATEGORY_TAB_SLOTS[i]) {
                if (categories[i] != holder.getCategory()) {
                    guideGUI.openEntries(player, categories[i]);
                }
                return;
            }
        }

        int index = slot - GuideGUI.ENTRY_START_SLOT;
        if (index < 0 || index >= holder.getEntries().size()) {
            return;
        }
        guideGUI.openBook(player, holder.getEntries().get(index));
    }
}
