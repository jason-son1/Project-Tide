package com.tide.core.tide;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class TideBossBarListener implements Listener {

    private final TideScheduler scheduler;

    public TideBossBarListener(TideScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduler.addPlayerToBossBar(event.getPlayer());
    }
}
