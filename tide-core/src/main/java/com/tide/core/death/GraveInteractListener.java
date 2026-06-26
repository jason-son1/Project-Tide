package com.tide.core.death;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public final class GraveInteractListener implements Listener {

    private final GraveManager graveManager;

    public GraveInteractListener(GraveManager graveManager) {
        this.graveManager = graveManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand armorStand)) {
            return;
        }
        WreckageGrave grave = graveManager.getGraveByStand(armorStand.getUniqueId());
        if (grave == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (graveManager.recover(player, grave)) {
            player.sendMessage("§a유실물을 회수했습니다.");
        } else {
            player.sendMessage("§7다른 모험가의 유산입니다.");
        }
    }
}
