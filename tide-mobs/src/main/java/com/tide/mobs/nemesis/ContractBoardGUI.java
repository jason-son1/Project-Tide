package com.tide.mobs.nemesis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ContractBoardGUI {

    private final ContractManager contractManager;

    public ContractBoardGUI(ContractManager contractManager) {
        this.contractManager = contractManager;
    }

    public void open(Player viewer) {
        Holder holder = new Holder();
        Inventory inventory = Bukkit.createInventory(holder, 27, "§4현상금 대행 게시판");
        holder.inventory = inventory;

        List<ContractRecord> contracts = contractManager.openContracts();
        holder.contracts = contracts;
        for (int i = 0; i < contracts.size(); i++) {
            int slot = i;
            if (slot >= 8) {
                slot++;
            }
            if (slot >= 27) {
                break;
            }
            inventory.setItem(slot, render(contracts.get(i)));
        }

        ItemStack guideBook = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta guideMeta = guideBook.getItemMeta();
        if (guideMeta != null) {
            guideMeta.setDisplayName("§a📖 계약 가이드 보기");
            guideMeta.setLore(List.of("§7클릭하면 계약 및 현상금 가이드를 엽니다."));
            guideBook.setItemMeta(guideMeta);
        }
        inventory.setItem(8, guideBook);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        viewer.openInventory(inventory);
    }

    private ItemStack render(ContractRecord contract) {
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta meta = itemStack.getItemMeta();
        OfflinePlayer poster = Bukkit.getOfflinePlayer(contract.getPosterUuid());
        meta.setDisplayName("§4[현상금] §f" + contract.getNemesisName());
        meta.setLore(List.of(
                "§7의뢰인: §f" + poster.getName(),
                "§7선불금(이미 지급됨): §6" + contract.getUpfrontClam() + " 조개",
                "§7성공보수: §d" + contract.getRewardPearl() + " 진주",
                "§e클릭하여 계약 수락"
        ));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private List<ContractRecord> contracts = List.of();

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public List<ContractRecord> getContracts() {
            return contracts;
        }
    }
}
