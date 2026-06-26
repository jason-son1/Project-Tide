package com.tide.mobs.nemesis;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class ContractBoardListener implements Listener {

    private final ContractManager contractManager;

    public ContractBoardListener(ContractManager contractManager) {
        this.contractManager = contractManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ContractBoardGUI.Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        var contracts = holder.getContracts();
        if (slot < 0 || slot >= contracts.size()) {
            return;
        }
        ContractRecord contract = contracts.get(slot);
        if (contractManager.accept(player, contract.getContractId())) {
            event.getInventory().setItem(slot, null);
        }
    }
}
