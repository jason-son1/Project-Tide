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

    @EventHandler
    public void onTideChange(TideChangeEvent event) {
        String effectKey;
        switch (event.getNewState()) {
            case HIGH_TIDE -> effectKey = "tide_change_high";
            case LOW_TIDE -> effectKey = "tide_change_low";
            case SPRING_TIDE -> effectKey = "tide_change_spring";
            case BLOOD_MOON -> effectKey = "tide_change_blood_moon";
            case BLOOD_TIDE -> effectKey = "tide_change_blood_tide";
            default -> {
                return;
            }
        }

        com.tide.core.effect.EffectEngine effectEngine = com.tide.core.TideCorePlugin.getInstance().getEffectEngine();
        if (effectEngine != null) {
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                effectEngine.playEffect(player, effectKey);
            }
        }
    }
}
