package com.tide.mobs.quest;

import com.tide.rpg.TideKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public final class BountyKillListener implements Listener {

    private final BountyManager bountyManager;

    public BountyKillListener(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        var pdc = event.getEntity().getPersistentDataContainer();
        boolean isElite = pdc.getOrDefault(TideKeys.ELITE, PersistentDataType.BYTE, (byte) 0) == 1;
        if (!isElite) {
            return;
        }
        String affixes = pdc.get(TideKeys.AFFIXES, PersistentDataType.STRING);
        bountyManager.onEliteKill(killer, true, affixes);
    }
}
