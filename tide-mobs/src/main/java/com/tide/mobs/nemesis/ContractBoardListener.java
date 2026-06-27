package com.tide.mobs.nemesis;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        if (slot == 8) {
            com.tide.core.TideCorePlugin corePlugin = com.tide.core.TideCorePlugin.getPlugin(com.tide.core.TideCorePlugin.class);
            new com.tide.core.guide.GuideGUI(corePlugin.getGuideRegistry()).openEntries(player, com.tide.core.guide.GuideCategory.BOUNTY);
            return;
        }

        var contracts = holder.getContracts();
        int contractIndex = slot;
        if (contractIndex >= 8) {
            contractIndex--;
        }
        if (contractIndex < 0 || contractIndex >= contracts.size()) {
            return;
        }
        ContractRecord contract = contracts.get(contractIndex);
        if (contractManager.accept(player, contract.getContractId())) {
            ItemStack filler = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fillerMeta = filler.getItemMeta();
            if (fillerMeta != null) {
                fillerMeta.setDisplayName(" ");
                filler.setItemMeta(fillerMeta);
            }
            event.getInventory().setItem(slot, filler);
        }
    }
}
