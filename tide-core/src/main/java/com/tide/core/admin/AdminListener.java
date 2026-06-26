package com.tide.core.admin;

import com.tide.core.economy.EconomyAPI;
import com.tide.core.reload.ReloadManager;
import com.tide.core.tide.TideScheduler;
import com.tide.core.tide.TideState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public final class AdminListener implements Listener {

    private final AdminGUI adminGUI;
    private final TideScheduler scheduler;
    private final ReloadManager reloadManager;
    private final EconomyAPI economyAPI;

    public AdminListener(AdminGUI adminGUI, TideScheduler scheduler, ReloadManager reloadManager, EconomyAPI economyAPI) {
        this.adminGUI = adminGUI;
        this.scheduler = scheduler;
        this.reloadManager = reloadManager;
        this.economyAPI = economyAPI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player admin)) {
            return;
        }

        switch (event.getRawSlot()) {
            case AdminGUI.HIGH_TIDE_BUTTON -> scheduler.forceState(TideState.HIGH_TIDE);
            case AdminGUI.LOW_TIDE_BUTTON -> scheduler.forceState(TideState.LOW_TIDE);
            case AdminGUI.SPRING_TIDE_BUTTON -> scheduler.forceState(TideState.SPRING_TIDE, 5 * 60L);
            case AdminGUI.BLOOD_MOON_BUTTON -> scheduler.forceState(TideState.BLOOD_MOON);
            case AdminGUI.RELOAD_ITEMS_BUTTON -> doReload(admin, "items");
            case AdminGUI.RELOAD_AFFIXES_BUTTON -> doReload(admin, "affixes");
            case AdminGUI.RELOAD_RUNES_BUTTON -> doReload(admin, "runes");
            case AdminGUI.RELOAD_ALL_BUTTON -> reloadAll(admin);
            default -> {
                if (event.getRawSlot() >= AdminGUI.PLAYER_ROW_START && event.getRawSlot() <= AdminGUI.PLAYER_ROW_END) {
                    showPlayerInfo(admin, event.getCurrentItem());
                    return;
                }
                return;
            }
        }
        adminGUI.render(event.getInventory());
    }

    private void doReload(Player admin, String target) {
        Integer count = reloadManager.reload(target);
        admin.sendMessage(count == null
                ? "§c리로드 대상이 없습니다: " + target
                : "§a[리로드] " + target + " §7- " + count + "건 완료");
    }

    private void reloadAll(Player admin) {
        reloadManager.reloadAll().forEach((name, count) ->
                admin.sendMessage("§a[리로드] " + name + " §7- " + count + "건"));
    }

    private void showPlayerInfo(Player admin, ItemStack head) {
        if (head == null || head.getItemMeta() == null) {
            return;
        }
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta.getOwningPlayer() == null) {
            return;
        }
        var uuid = meta.getOwningPlayer().getUniqueId();
        admin.sendMessage("§6--- " + meta.getOwningPlayer().getName() + " ---");
        admin.sendMessage("§7조개: §f" + economyAPI.getClam(uuid)
                + " §7진주: §f" + economyAPI.getPearl(uuid)
                + " §7평판: §f" + economyAPI.getRep(uuid) + " (" + economyAPI.getRepTier(uuid) + ")");
    }
}
