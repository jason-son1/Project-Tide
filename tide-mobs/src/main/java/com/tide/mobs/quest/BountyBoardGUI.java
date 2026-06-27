package com.tide.mobs.quest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class BountyBoardGUI {

    private final BountyManager bountyManager;

    public BountyBoardGUI(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inventory = Bukkit.createInventory(holder, 27, "§6현상금 보드");
        holder.inventory = inventory;

        List<BountyQuest> quests = bountyManager.getQuests(player);
        holder.quests = quests;
        for (int i = 0; i < quests.size() && i < 27; i++) {
            inventory.setItem(i * 2 + 1 <= 26 ? i * 2 + 1 : i, renderQuest(quests.get(i)));
        }

        ItemStack guideBook = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta guideMeta = guideBook.getItemMeta();
        if (guideMeta != null) {
            guideMeta.setDisplayName("§a📖 현상금 가이드 보기");
            guideMeta.setLore(List.of("§7클릭하면 현상금 가이드를 엽니다."));
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

        player.openInventory(inventory);
    }

    public ItemStack renderQuest(BountyQuest quest) {
        Material material = quest.isClaimed() ? Material.GRAY_DYE
                : quest.isComplete() ? Material.EMERALD : Material.PAPER;
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(quest.describe());
        meta.setLore(List.of(
                "§7진행도: §f" + quest.getProgress() + " / " + quest.getTargetCount(),
                "§7보상: §6조개 " + quest.getRewardClam() + " §7+ §a평판 " + quest.getRewardRep(),
                quest.isClaimed() ? "§7(수령 완료)" : quest.isComplete() ? "§e클릭하여 수령" : "§7진행 중"
        ));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private List<BountyQuest> quests = List.of();

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public List<BountyQuest> getQuests() {
            return quests;
        }
    }
}
