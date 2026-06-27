package com.tide.rpg.forge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

public final class ForgeGUI {

    public static final int TAB_REINFORCE_BUTTON = 0;
    public static final int TAB_SOCKET_BUTTON = 1;
    public static final int TAB_REROLL_BUTTON = 2;
    public static final int TAB_FUSION_BUTTON = 3;
    public static final int GUIDE_BUTTON_SLOT = 8;

    public static final int REINFORCE_GEAR_SLOT = 13;
    public static final int REINFORCE_STONE_SLOT = 11;
    public static final int REINFORCE_SCROLL_SLOT = 15;
    public static final int REINFORCE_INFO_SLOT = 29;
    public static final int REINFORCE_ATTEMPT_BUTTON = 31;

    public static final int SOCKET_GEAR_SLOT = 13;
    public static final int SOCKET_1_SLOT = 10;
    public static final int SOCKET_2_SLOT = 11;
    public static final int SOCKET_3_SLOT = 12;
    public static final int SOCKET_INFO_SLOT = 29;
    public static final int SOCKET_ATTACH_BUTTON = 31;

    public static final int REROLL_GEAR_SLOT = 13;
    public static final int REROLL_BUTTON = 22;
    public static final int REROLL_CURRENT_INFO_SLOT = 24;
    public static final int REROLL_POOL_INFO_SLOT = 26;

    public static final int FUSION_MATERIAL_1_SLOT = 10;
    public static final int FUSION_MATERIAL_2_SLOT = 11;
    public static final int FUSION_MATERIAL_3_SLOT = 12;
    public static final int FUSION_BUTTON = 22;
    public static final int FUSION_RESULT_INFO_SLOT = 24;

    public static Set<Integer> placementSlots(ForgeTab tab) {
        return switch (tab) {
            case REINFORCE -> Set.of(REINFORCE_GEAR_SLOT, REINFORCE_STONE_SLOT, REINFORCE_SCROLL_SLOT);
            case SOCKET -> Set.of(SOCKET_GEAR_SLOT, SOCKET_1_SLOT, SOCKET_2_SLOT, SOCKET_3_SLOT);
            case REROLL -> Set.of(REROLL_GEAR_SLOT);
            case FUSION -> Set.of(FUSION_MATERIAL_1_SLOT, FUSION_MATERIAL_2_SLOT, FUSION_MATERIAL_3_SLOT);
        };
    }

    public void open(Player player) {
        ForgeHolder holder = new ForgeHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, "§6The Tide 대장간");
        holder.setInventory(inventory);
        render(inventory, ForgeTab.REINFORCE);
        player.openInventory(inventory);
    }

    public void render(Inventory inventory, ForgeTab tab) {
        inventory.clear();
        placeTabButtons(inventory, tab);

        switch (tab) {
            case REINFORCE -> renderReinforce(inventory);
            case SOCKET -> renderSocket(inventory);
            case REROLL -> renderReroll(inventory);
            case FUSION -> renderFusion(inventory);
        }

        fillEmpty(inventory);
    }

    private void placeTabButtons(Inventory inventory, ForgeTab current) {
        inventory.setItem(TAB_REINFORCE_BUTTON, button(Material.ANVIL, "§6⚒ 강화", current == ForgeTab.REINFORCE));
        inventory.setItem(TAB_SOCKET_BUTTON, button(Material.AMETHYST_SHARD, "§d💎 룬 장착", current == ForgeTab.SOCKET));
        inventory.setItem(TAB_REROLL_BUTTON, button(Material.EXPERIENCE_BOTTLE, "§b🔄 룬 리롤", current == ForgeTab.REROLL));
        inventory.setItem(TAB_FUSION_BUTTON, button(Material.BLAZE_POWDER, "§c🔗 룬 융합", current == ForgeTab.FUSION));
        inventory.setItem(GUIDE_BUTTON_SLOT, marker(Material.WRITTEN_BOOK, "§a📖 강화 가이드 보기", "§7클릭하면 장비 강화 가이드를 엽니다."));
    }

    private void renderReinforce(Inventory inventory) {
        inventory.setItem(REINFORCE_STONE_SLOT, marker(Material.QUARTZ, "§7강화석을 놓으세요"));
        inventory.setItem(REINFORCE_GEAR_SLOT, marker(Material.IRON_SWORD, "§7강화할 장비를 놓으세요"));
        inventory.setItem(REINFORCE_SCROLL_SLOT, marker(Material.PAPER, "§7보호권 (선택)"));
        inventory.setItem(REINFORCE_INFO_SLOT, marker(Material.BOOK, "§e강화 정보", "§7장비를 배치하면 표시됩니다"));
        inventory.setItem(REINFORCE_ATTEMPT_BUTTON, button(Material.EMERALD, "§a[강화 시도]", false));
    }

    private void renderSocket(Inventory inventory) {
        inventory.setItem(SOCKET_GEAR_SLOT, marker(Material.IRON_SWORD, "§7장비를 놓으세요"));
        inventory.setItem(SOCKET_1_SLOT, marker(Material.GRAY_STAINED_GLASS_PANE, "§7소켓 1 - 룬을 놓으세요"));
        inventory.setItem(SOCKET_2_SLOT, marker(Material.GRAY_STAINED_GLASS_PANE, "§7소켓 2 - 룬을 놓으세요"));
        inventory.setItem(SOCKET_3_SLOT, marker(Material.GRAY_STAINED_GLASS_PANE, "§7소켓 3 - 룬을 놓으세요"));
        inventory.setItem(SOCKET_INFO_SLOT, marker(Material.BOOK, "§e룬 장착 정보", "§7장비를 배치하면 표시됩니다"));
        inventory.setItem(SOCKET_ATTACH_BUTTON, button(Material.EMERALD, "§a[장착]", false));
    }

    private void renderReroll(Inventory inventory) {
        inventory.setItem(REROLL_GEAR_SLOT, marker(Material.IRON_SWORD, "§7장비를 놓으세요"));
        inventory.setItem(REROLL_BUTTON, button(Material.EXPERIENCE_BOTTLE, "§b[리롤]", false));
        inventory.setItem(REROLL_CURRENT_INFO_SLOT, marker(Material.BOOK, "§e현재 소켓", "§7장비를 배치하면 표시됩니다"));
        inventory.setItem(REROLL_POOL_INFO_SLOT, marker(Material.WRITTEN_BOOK, "§e리롤 풀", "§7무작위 등급 1~2 룬"));
    }

    private void renderFusion(Inventory inventory) {
        inventory.setItem(FUSION_MATERIAL_1_SLOT, marker(Material.AMETHYST_SHARD, "§7재료 룬 1"));
        inventory.setItem(FUSION_MATERIAL_2_SLOT, marker(Material.AMETHYST_SHARD, "§7재료 룬 2"));
        inventory.setItem(FUSION_MATERIAL_3_SLOT, marker(Material.AMETHYST_SHARD, "§7재료 룬 3"));
        inventory.setItem(FUSION_BUTTON, button(Material.EMERALD, "§a[융합]", false));
        inventory.setItem(FUSION_RESULT_INFO_SLOT, marker(Material.BOOK, "§e결과 미리보기", "§7동일한 룬 3개를 놓으세요"));
    }

    private void fillEmpty(Inventory inventory) {
        ItemStack filler = marker(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private ItemStack button(Material material, String name, boolean active) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(active ? name + " §a✔" : name);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack marker(Material material, String name, String... lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(java.util.List.of(lore));
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
