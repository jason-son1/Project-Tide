package com.tide.core.admin;

import com.tide.core.TideCorePlugin;
import com.tide.core.economy.EconomyManager;
import com.tide.core.tide.BountyTempoProvider;
import com.tide.core.tide.TideScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

/** 54-slot layout exactly per the 3-3 spec: tide control / economy monitor / reload / player rows. */
public final class AdminGUI {

    public static final int TITLE_SLOT = 4;

    public static final int HIGH_TIDE_BUTTON = 9;
    public static final int LOW_TIDE_BUTTON = 10;
    public static final int SPRING_TIDE_BUTTON = 11;
    public static final int BLOOD_MOON_BUTTON = 12;
    public static final int CURRENT_STATE_SLOT = 13;
    public static final int COUNTDOWN_SLOT = 14;
    public static final int TIDE_TEMPO_SLOT = 15;
    public static final int BOUNTY_DAILY_TEMPO_SLOT = 16;
    public static final int BOUNTY_WEEKLY_TEMPO_SLOT = 17;

    public static final int TOTAL_CLAM_SLOT = 18;
    public static final int TOTAL_PEARL_SLOT = 19;
    public static final int INFLATION_SLOT = 20;

    public static final int RELOAD_ITEMS_BUTTON = 27;
    public static final int RELOAD_AFFIXES_BUTTON = 28;
    public static final int RELOAD_RUNES_BUTTON = 29;
    public static final int RELOAD_ALL_BUTTON = 30;

    public static final int PLAYER_ROW_START = 36;
    public static final int PLAYER_ROW_END = 44;

    private final TideCorePlugin plugin;
    private final TideScheduler scheduler;
    private final EconomyManager economyManager;

    public AdminGUI(TideCorePlugin plugin, TideScheduler scheduler, EconomyManager economyManager) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.economyManager = economyManager;
    }

    public void open(Player viewer) {
        AdminHolder holder = new AdminHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, "§6§lThe Tide 관리자 패널");
        holder.setInventory(inventory);
        render(inventory);
        viewer.openInventory(inventory);
    }

    public void render(Inventory inventory) {
        inventory.setItem(TITLE_SLOT, named(Material.NAME_TAG, "§6§lThe Tide 관리자 패널"));

        inventory.setItem(HIGH_TIDE_BUTTON, named(Material.PRISMARINE_CRYSTALS, "§b🌊 밀물로 변경"));
        inventory.setItem(LOW_TIDE_BUTTON, named(Material.PRISMARINE_SHARD, "§9💨 썰물로 변경"));
        inventory.setItem(SPRING_TIDE_BUTTON, named(Material.NAUTILUS_SHELL, "§e🌟 사리 발동 §7(5분 한정)"));
        inventory.setItem(BLOOD_MOON_BUTTON, named(Material.REDSTONE, "§c🩸 블러드문 발동"));
        inventory.setItem(CURRENT_STATE_SLOT, named(Material.GLASS_PANE,
                "§f현재 상태: " + scheduler.getCurrentState().getDisplayName()));
        inventory.setItem(COUNTDOWN_SLOT, named(Material.CLOCK,
                "§f다음 전환까지: §e" + formatSeconds(scheduler.getSecondsUntilNextChange())));

        RegisteredServiceProvider<BountyTempoProvider> rsp = Bukkit.getServicesManager().getRegistration(BountyTempoProvider.class);
        BountyTempoProvider bountyProvider = rsp != null ? rsp.getProvider() : null;

        inventory.setItem(TIDE_TEMPO_SLOT, named(Material.CLOCK, "§b⏱ 조수 주기 설정",
                "§7현재 주기: §f" + scheduler.getCycleDurationMinutes() + "분",
                "§7좌클릭: §a+10분 §7| 우클릭: §c-10분",
                "§7쉬프트+좌클릭: §a+60분 §7| 쉬프트+우클릭: §c-60분"
        ));

        long dailyMin = bountyProvider != null ? bountyProvider.getDailyResetIntervalMinutes() : 1440;
        inventory.setItem(BOUNTY_DAILY_TEMPO_SLOT, named(Material.WRITABLE_BOOK, "§d⏱ 현상금 일일 주기 설정",
                "§7현재 주기: §f" + dailyMin + "분",
                "§7좌클릭: §a+10분 §7| 우클릭: §c-10분",
                "§7쉬프트+좌클릭: §a+60분 §7| 쉬프트+우클릭: §c-60분"
        ));

        long weeklyMin = bountyProvider != null ? bountyProvider.getWeeklyResetIntervalMinutes() : 10080;
        inventory.setItem(BOUNTY_WEEKLY_TEMPO_SLOT, named(Material.BOOK, "§d⏱ 현상금 주간 주기 설정",
                "§7현재 주기: §f" + weeklyMin + "분",
                "§7좌클릭: §a+10분 §7| 우클릭: §c-10분",
                "§7쉬프트+좌클릭: §a+60분 §7| 쉬프트+우클릭: §c-60분"
        ));

        inventory.setItem(TOTAL_CLAM_SLOT, named(Material.GOLD_INGOT,
                "§6서버 총 조개(온라인): §f" + economyManager.getOnlineClamTotal()));
        inventory.setItem(TOTAL_PEARL_SLOT, named(Material.PRISMARINE_CRYSTALS,
                "§d서버 총 진주(온라인): §f0"));
        inventory.setItem(INFLATION_SLOT, inflationIndicator());

        inventory.setItem(RELOAD_ITEMS_BUTTON, named(Material.PAPER, "§a[아이템 리로드]"));
        inventory.setItem(RELOAD_AFFIXES_BUTTON, named(Material.ZOMBIE_HEAD, "§a[몹 접두사 리로드]"));
        inventory.setItem(RELOAD_RUNES_BUTTON, named(Material.AMETHYST_SHARD, "§a[룬 리로드]"));
        inventory.setItem(RELOAD_ALL_BUTTON, named(Material.EMERALD_BLOCK, "§a[전체 리로드]"));

        int slot = PLAYER_ROW_START;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot > PLAYER_ROW_END) {
                break;
            }
            inventory.setItem(slot, playerHead(online));
            slot++;
        }

        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack inflationIndicator() {
        long threshold = plugin.getConfig().getLong("economy.inflation-alert-threshold", 1_000_000);
        long total = economyManager.getOnlineClamTotal();
        Material wool;
        String label;
        if (total < threshold * 0.7) {
            wool = Material.LIME_WOOL;
            label = "§a정상";
        } else if (total < threshold) {
            wool = Material.YELLOW_WOOL;
            label = "§e주의";
        } else {
            wool = Material.RED_WOOL;
            label = "§c인플레이션 경고";
        }
        return named(wool, "§f인플레 상태: " + label);
    }

    private ItemStack playerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName("§f" + player.getName());
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && lore.length > 0) {
            meta.setLore(java.util.List.of(lore));
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private String formatSeconds(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
