package com.tide.core.menu;

import com.tide.core.economy.EconomyAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;

public class MainMenuGUI {

    private final EconomyAPI economyAPI;

    public MainMenuGUI(EconomyAPI economyAPI) {
        this.economyAPI = economyAPI;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, "🌊 The Tide — 메인 메뉴");

        // Decorative borders (Gray Stained Glass Panes)
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }

        // Fill top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 18, border);
        }
        inventory.setItem(17, border);

        // Slot 9: Guide / Tutorial
        inventory.setItem(9, createItem(Material.KNOWLEDGE_BOOK, "§b§l📖 서버 가이드",
                "§7",
                "§7조수, 강화, 네메시스 등 서버의",
                "§7모든 시스템을 분류별로 설명합니다.",
                "§f",
                "§e▶ 클릭하여 열기"
        ));

        // Slot 10: Bounty Board
        inventory.setItem(10, createItem(Material.WRITABLE_BOOK, "§e§l📜 현상금 보드",
                "§7",
                "§7일일/주간 현상금 임무를 확인하고",
                "§7수행하여 명성 및 특별 보상을 획득합니다.",
                "§f",
                "§e▶ 클릭하여 열기"
        ));

        // Slot 11: Forge
        inventory.setItem(11, createItem(Material.ANVIL, "§a§l🔨 통합 대장간",
                "§7",
                "§7장비를 강화하거나, 장비 소켓에 룬을 장착,",
                "§7합성 및 리롤하여 더욱 강력하게 단련합니다.",
                "§f",
                "§e▶ 클릭하여 열기"
        ));

        // Slot 12: Shop
        inventory.setItem(12, createItem(Material.GOLD_INGOT, "§b§l🪙 조개 상점",
                "§7",
                "§7모험과 낚시를 통해 획득한 재화로",
                "§7다양한 무기, 방어구, 강화 주문서를 구매합니다.",
                "§f",
                "§e▶ 클릭하여 열기"
        ));

        // Slot 13: Player Head Stats
        inventory.setItem(13, createPlayerHead(player));

        // Slot 14: Deep Mine
        inventory.setItem(14, createItem(Material.IRON_PICKAXE, "§d§l⛏️ 딥 마인 광산",
                "§7",
                "§7위험천만하고 가치 있는 심층 광산으로 이동합니다.",
                "§7광산이 리셋될 때마다 새로운 광물이 채워집니다.",
                "§f",
                "§e▶ 클릭하여 이동"
        ));

        // Slot 15: Sell All
        inventory.setItem(15, createItem(Material.CHEST, "§6§l💰 소지품 전체 판매",
                "§7",
                "§7인벤토리 안의 판매 가능한 전리품을",
                "§7한 번에 조개(Clam)로 환전합니다.",
                "§f",
                "§e▶ 클릭하여 즉시 판매"
        ));

        // Slot 16: Admin Settings (visible/active if op or permission)
        if (player.isOp() || player.hasPermission("tide.admin")) {
            inventory.setItem(16, createItem(Material.COMMAND_BLOCK, "§c§l⚙️ 관리자 설정",
                    "§7",
                    "§7서버 환경 설정, 조수 스케줄링 관리,",
                    "§7데이터베이스 경제 수정 및 핫 리로드를 수행합니다.",
                    "§f",
                    "§e▶ 클릭하여 관리자 패널 열기"
            ));
        } else {
            inventory.setItem(16, border);
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName("§f§l👤 " + player.getName() + "님의 정보");

            long clam = economyAPI.getClam(player.getUniqueId());
            long pearl = economyAPI.getPearl(player.getUniqueId());
            int rep = economyAPI.getRep(player.getUniqueId());
            String repTier = economyAPI.getRepTier(player.getUniqueId()).name();
            boolean hardMode = economyAPI.isHardMode(player.getUniqueId());

            meta.setLore(Arrays.asList(
                    "§7",
                    "§7• 보유 조개: §b" + clam + " 🪙",
                    "§7• 보유 진주: §d" + pearl + " 🔮",
                    "§7• 평판 등급: §e" + repTier + " §7(" + rep + "점)",
                    "§7• 하드코어 상태: " + (hardMode ? "§c❤️ HARDCORE" : "§aNORMAL"),
                    "§7"
            ));
            head.setItemMeta(meta);
        }
        return head;
    }
}
