package com.tide.mobs.nemesis;

import com.tide.core.economy.EconomyAPI;
import com.tide.rpg.item.ItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.List;

public final class NemesisRewardListener implements Listener {

    private final NemesisManager nemesisManager;
    private final EconomyAPI economyAPI;
    private final ItemFactory itemFactory;

    public NemesisRewardListener(NemesisManager nemesisManager, EconomyAPI economyAPI, ItemFactory itemFactory) {
        this.nemesisManager = nemesisManager;
        this.economyAPI = economyAPI;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        NemesisRecord record = nemesisManager.findByMob(event.getEntity().getUniqueId());
        if (record == null) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.getUniqueId().equals(record.getPlayerUuid())) {
            return;
        }

        List<org.bukkit.inventory.ItemStack> originalDrops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        for (int i = 0; i < 3; i++) {
            event.getDrops().addAll(originalDrops);
        }
        if (itemFactory != null) {
            event.getDrops().add(itemFactory.create("nemesis_token"));
        }

        economyAPI.addRep(killer.getUniqueId(), 50);
        killer.sendTitle("§a[복수 완료]", "§f" + record.getOriginalName() + "을(를) 쓰러뜨렸습니다!", 10, 70, 20);
        nemesisManager.deactivate(record);
    }
}
