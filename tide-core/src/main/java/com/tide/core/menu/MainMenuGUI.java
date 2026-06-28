package com.tide.core.menu;

import com.tide.core.economy.EconomyAPI;
import com.tide.core.TideCorePlugin;
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
        // Slot 17: Go to Lobby
        inventory.setItem(17, createItem(Material.NETHER_STAR, "§b§l✨ 로비로 이동",
                "§7",
                "§7안전하고 평화로운 로비 구역으로 이동합니다.",
                "§7서바이벌 모드에서는 §e5초§7간 제자리에 대기해야 합니다.",
                "§f",
                "§e▶ 클릭하여 이동"
        ));

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

        // Slot 14: Tide State details
        com.tide.core.tide.TideScheduler scheduler = TideCorePlugin.getInstance().getTideScheduler();
        com.tide.core.tide.TideState state = scheduler.getCurrentState();
        long secondsRemaining = scheduler.getSecondsUntilNextChange();
        long hours = secondsRemaining / 3600;
        long minutes = (secondsRemaining % 3600) / 60;
        long seconds = secondsRemaining % 60;
        String timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        java.util.List<String> tideLore = new java.util.ArrayList<>();
        tideLore.add("§7");
        tideLore.add("§7• 현재 상태: " + state.getDisplayName());
        tideLore.add("§7• 남은 시간: §f" + timeStr);
        tideLore.add("§7");
        tideLore.add("§e[현재 조수의 효과]");
        switch (state) {
            case HIGH_TIDE -> {
                tideLore.add("§7🌊 낚시 찌 대기 속도가 §a20%§7 단축됩니다.");
                tideLore.add("§7🌊 바다/강 생물 처치 시 특수 전리품 획득율이 §a+15%§7 증가합니다.");
            }
            case LOW_TIDE -> {
                tideLore.add("§7💨 딥 마인 던전에서 광석 채굴 시 드롭량이 §a+15%§7 증가합니다.");
                tideLore.add("§7💨 물이 빠져 낚시 찌 대기 시간이 §c+10%§7 증가합니다.");
            }
            case SPRING_TIDE -> {
                tideLore.add("§7🌟 조수 간만의 차가 극에 달해 몬스터 체력/공격력이 §c+25%§7 증가합니다.");
                tideLore.add("§7🌟 일반/엘리트 몹 처치 시 명성(평판) 및 드롭율이 §a+50%§7 보너스를 얻습니다.");
            }
            case BLOOD_MOON -> {
                tideLore.add("§7🩸 세상을 붉게 물들이는 달의 기운으로 모든 일반 몹이 엘리트(Elite)로 스폰될 확률이 §c+30%§7 증가합니다.");
                tideLore.add("§7🩸 사망 패널티가 강화되어 무덤 생성 및 경험치 복구 비용이 §c2배§7가 됩니다.");
            }
            case BLOOD_TIDE -> {
                tideLore.add("§7🩸🌟 심연의 힘과 달의 저주가 겹쳐 가장 위험한 밤이 찾아옵니다.");
                tideLore.add("§7🩸🌟 모든 몬스터의 공격력이 §c+50%§7 증가하며 타격 시 무작위 디버프를 부여합니다.");
                tideLore.add("§7🩸🌟 그에 비례해 엘리트 및 네메시스 보스의 전리품 드롭율이 §a+100%§7 폭증합니다.");
            }
        }
        tideLore.add("§7");
        tideLore.add("§8— 시간에 따라 변화하는 바다의 흐름을 주기적으로 관찰하세요 —");

        inventory.setItem(14, createItem(Material.HEART_OF_THE_SEA, "§b§l🌊 현재 조수 상태", tideLore.toArray(new String[0])));
        inventory.setItem(15, border);

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

            java.util.List<String> lore = new java.util.ArrayList<>(java.util.List.of(
                    "§7",
                    "§7• 보유 조개: §b" + clam + " 🪙",
                    "§7• 보유 진주: §d" + pearl + " 🔮",
                    "§7• 평판 등급: §e" + repTier + " §7(" + rep + "점)",
                    "§7• 하드코어 상태: " + (hardMode ? "§c❤️ HARDCORE" : "§aNORMAL")
            ));

            int peakGs = economyAPI.getPeakGearScore(player.getUniqueId());
            double pi = economyAPI.getProgressionIndex(player.getUniqueId());
            lore.add("§7• 최고 전투력(Peak GS): §f" + peakGs);
            lore.add("§7• 진행 지표(PI): §f" + String.format("%.1f", pi));

            var difficultyManager = Bukkit.getServicesManager().load(com.tide.core.difficulty.DifficultyManager.class);
            if (difficultyManager != null && difficultyManager.isEnabled()) {
                var result = difficultyManager.resolve(player.getLocation());
                lore.add("§7• 현재 위치 난이도 등급: §e" + result.bracket().id().toUpperCase());
            }
            lore.add("§8(정확한 현재 장비 전투력은 §f/power§8 명령어로 확인)");
            lore.add("§7");

            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }
}
