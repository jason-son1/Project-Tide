package com.tide.rpg.gs;

import com.tide.core.economy.EconomyAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Recomputes GearScore on every action that can change equipped gear and
 * pushes it to EconomyAPI.updatePeakGearScoreIfHigher — the peak value (used
 * by Progression Index) only ever rises, so unequipping gear to dodge
 * difficulty scaling never lowers it back down.
 */
public final class PeakGearScoreListener implements Listener {

    private final GearScoreCalculator gearScoreCalculator;
    private final EconomyAPI economyAPI;

    public PeakGearScoreListener(GearScoreCalculator gearScoreCalculator, EconomyAPI economyAPI) {
        this.gearScoreCalculator = gearScoreCalculator;
        this.economyAPI = economyAPI;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        track(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            track(player);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        track(event.getPlayer());
    }

    private void track(Player player) {
        int gearScore = gearScoreCalculator.calculate(player);
        economyAPI.updatePeakGearScoreIfHigher(player.getUniqueId(), gearScore);
    }
}
