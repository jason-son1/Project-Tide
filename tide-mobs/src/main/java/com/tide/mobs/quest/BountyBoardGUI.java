package com.tide.mobs.quest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
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

        // Place quest items in odd slots: 1, 3, 5, 7, 9, 11, …
        for (int i = 0; i < quests.size() && i < 13; i++) {
            int slot = i * 2 + 1;
            if (slot >= inventory.getSize()) slot = i;
            inventory.setItem(slot, renderQuest(quests.get(i)));
        }

        // Guide book in slot 8
        ItemStack guideBook = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta guideMeta = guideBook.getItemMeta();
        if (guideMeta != null) {
            guideMeta.setDisplayName("§a📖 현상금 가이드 보기");
            guideMeta.setLore(List.of("§7클릭하면 현상금 가이드를 엽니다."));
            guideBook.setItemMeta(guideMeta);
        }
        inventory.setItem(8, guideBook);

        // Fill remaining slots with filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, filler);
        }

        player.openInventory(inventory);
    }

    public ItemStack renderQuest(BountyQuest quest) {
        Material mat = quest.isClaimed()  ? Material.GRAY_DYE
                     : quest.isComplete() ? Material.EMERALD
                     : quest.getIcon();
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(quest.describe());

        List<String> lore = new ArrayList<>();
        if (!quest.getDescription().isBlank()) {
            lore.add("§7" + quest.getDescription());
            lore.add("");
        }
        lore.add("§7진행도: §f" + quest.getProgress() + " / " + quest.getTargetCount());
        lore.add("§7보상: §6조개 " + quest.getRewardClam() + " §7+ §a평판 " + quest.getRewardRep());
        if (quest.isClaimed()) {
            lore.add("§7(수령 완료)");
        } else if (quest.isComplete()) {
            lore.add("§e▶ 클릭하여 보상 수령");
        } else {
            lore.add("§7진행 중...");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Holder ────────────────────────────────────────────────────────────────

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private List<BountyQuest> quests = List.of();

        @Override
        public Inventory getInventory() { return inventory; }
        public List<BountyQuest> getQuests() { return quests; }
    }
}
