package com.tide.mobs.nemesis;

import com.tide.core.economy.EconomyAPI;
import com.tide.rpg.item.ItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Whoever lands the kill always releases the curse and triggers Legacy Theft
 * recovery (3-2) — that's what lets a Bounty Contract mercenary (3-3) free
 * someone else's nemesis. The bonus loot/rep, though, still favors the
 * original target finishing their own fight.
 */
public final class NemesisRewardListener implements Listener {

    private final NemesisManager nemesisManager;
    private final EconomyAPI economyAPI;
    private final ItemFactory itemFactory;
    private final LegacyTheftManager legacyTheftManager;
    private final ContractManager contractManager;

    public NemesisRewardListener(NemesisManager nemesisManager, EconomyAPI economyAPI, ItemFactory itemFactory,
                                  LegacyTheftManager legacyTheftManager, ContractManager contractManager) {
        this.nemesisManager = nemesisManager;
        this.economyAPI = economyAPI;
        this.itemFactory = itemFactory;
        this.legacyTheftManager = legacyTheftManager;
        this.contractManager = contractManager;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        NemesisRecord record = nemesisManager.findByMob(event.getEntity().getUniqueId());
        if (record == null) {
            return;
        }

        legacyTheftManager.recover(event.getEntity().getUniqueId());

        Player killer = event.getEntity().getKiller();
        ContractRecord contract = contractManager.findByNemesisMob(event.getEntity().getUniqueId());
        boolean mercenarySucceeded = contract != null && killer != null
                && killer.getUniqueId().equals(contract.getMercenaryUuid());
        contractManager.resolve(event.getEntity().getUniqueId(), mercenarySucceeded);

        if (killer != null && killer.getUniqueId().equals(record.getPlayerUuid())) {
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
        } else if (killer != null) {
            economyAPI.addRep(killer.getUniqueId(), 20);
            killer.sendMessage("§a다른 모험가의 네메시스를 처치했습니다. §7(평판 +20)");
            Player originalOwner = org.bukkit.Bukkit.getPlayer(record.getPlayerUuid());
            if (originalOwner != null) {
                originalOwner.sendMessage("§e당신의 네메시스가 §f" + killer.getName() + "§e에 의해 처치되었습니다. 저주가 해제되었습니다.");
            }
        }

        nemesisManager.deactivate(record);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        legacyTheftManager.deliverPending(event.getPlayer());
    }
}
