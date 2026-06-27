package com.tide.core.menu;

import com.tide.core.TideCorePlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class MainMenuListener implements Listener {

    private final MainMenuGUI mainMenuGUI;

    public MainMenuListener(MainMenuGUI mainMenuGUI) {
        this.mainMenuGUI = mainMenuGUI;
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            event.setCancelled(true);
            mainMenuGUI.open(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("🌊 The Tide — 메인 메뉴")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        // Only handle clicks on the actual menu slots, not player inventory slots
        if (slot >= 27) {
            return;
        }

        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);

        switch (slot) {
            case 9 -> player.performCommand("guide");
            case 10 -> player.performCommand("bounty");
            case 11 -> player.performCommand("forge");
            case 12 -> player.performCommand("shop");
            case 16 -> {
                if (player.isOp() || player.hasPermission("tide.admin")) {
                    player.performCommand("tide admin");
                } else {
                    player.sendMessage("§c관리자 권한이 없습니다.");
                }
            }
            case 17 -> TideCorePlugin.getInstance().getLobbyManager().teleportToLobby(player);
        }
    }
}
