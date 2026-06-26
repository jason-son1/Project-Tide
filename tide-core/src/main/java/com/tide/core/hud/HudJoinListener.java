package com.tide.core.hud;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class HudJoinListener implements Listener {

    private final EconomyScoreboardHud hud;

    public HudJoinListener(EconomyScoreboardHud hud) {
        this.hud = hud;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        hud.setup(event.getPlayer());
    }
}
