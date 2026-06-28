package com.tide.rpg.codex;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles all click interactions inside the Codex GUI.
 * Tab switching (slots 0-4) and page navigation (slots 45, 53) are supported.
 */
public final class CodexListener implements Listener {

    private final CodexGUI codexGUI;
    private final RuneCodexGUI runeCodexGUI;

    public CodexListener(CodexGUI codexGUI, RuneCodexGUI runeCodexGUI) {
        this.codexGUI = codexGUI;
        this.runeCodexGUI = runeCodexGUI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof RuneCodexHolder) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player && event.getRawSlot() == 0) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.6f, 1.2f);
                codexGUI.open(player);
            }
            return;
        }

        if (!(event.getInventory().getHolder() instanceof CodexHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        CodexTab currentTab = holder.getTab();
        int currentPage = holder.getPage();

        // Tab buttons
        switch (slot) {
            case 0 -> { if (currentTab != CodexTab.ALL)        openTab(player, CodexTab.ALL,        0); return; }
            case 1 -> { if (currentTab != CodexTab.GEAR)       openTab(player, CodexTab.GEAR,       0); return; }
            case 2 -> { if (currentTab != CodexTab.CONSUMABLE) openTab(player, CodexTab.CONSUMABLE, 0); return; }
            case 3 -> { if (currentTab != CodexTab.DEEPMINE)   openTab(player, CodexTab.DEEPMINE,   0); return; }
            case 4 -> { if (currentTab != CodexTab.BOSS)       openTab(player, CodexTab.BOSS,       0); return; }
        }

        if (slot == 5) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.6f, 1.2f);
            runeCodexGUI.open(player);
            return;
        }

        if (slot == 8) {
            com.tide.core.TideCorePlugin corePlugin = com.tide.core.TideCorePlugin.getInstance();
            if (corePlugin != null && corePlugin.getGuideRegistry() != null) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                new com.tide.core.guide.GuideGUI(corePlugin.getGuideRegistry()).openCategories(player);
            } else {
                player.sendMessage("§c가이드를 로드할 수 없습니다.");
            }
            return;
        }

        // Page navigation
        if (slot == 45) {
            if (currentPage > 0) {
                openTab(player, currentTab, currentPage - 1);
            }
            return;
        }
        if (slot == 53) {
            openTab(player, currentTab, currentPage + 1);
            return;
        }

        // Item slots 9-44: show detail info in chat
        if (slot >= 9 && slot <= 44) {
            var clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;
            // Item information is already in the lore; just play a sound
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
        }
    }

    private void openTab(Player player, CodexTab tab, int page) {
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.6f, 1.2f);
        codexGUI.open(player, tab, page);
    }
}
