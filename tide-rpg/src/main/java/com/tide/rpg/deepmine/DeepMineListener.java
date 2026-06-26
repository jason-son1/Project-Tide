package com.tide.rpg.deepmine;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public final class DeepMineListener implements Listener {

    public static final String DEEPMINE_DEATH_METADATA = "tide_deepmine_death";

    private final DeepMineManager manager;
    private final Plugin plugin;

    public DeepMineListener(DeepMineManager manager, Plugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!manager.isInside(player.getLocation())) {
            return;
        }
        manager.trackLoot(player, event.getItem().getItemStack());
    }

    /**
     * LOWEST so this always runs before TideCore's WreckageGrave handler
     * (registered at NORMAL), regardless of plugin enable order.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!manager.isInside(player.getLocation())) {
            return;
        }
        player.setMetadata(DEEPMINE_DEATH_METADATA, new FixedMetadataValue(plugin, true));
        manager.onDeathInside(player);
    }
}
