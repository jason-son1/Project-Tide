package com.tide.rpg.codex;

import com.tide.core.guide.CodexOpener;
import com.tide.rpg.item.ItemDefinition;
import com.tide.rpg.item.ItemFactory;
import com.tide.rpg.item.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 아이템 도감(Codex) GUI.
 * /codex 명령어로 열 수 있으며, 등록된 모든 커스텀 아이템의 정보를 확인할 수 있습니다.
 *
 * GUI 레이아웃 (54칸):
 *   슬롯 0–7  : 탭 버튼 (전체/장비/소모품/딥마인/보스)
 *   슬롯 8    : 이전 페이지
 *   슬롯 35   : 다음 페이지 (실제로는 53에 배치)
 *   슬롯 9–44 : 아이템 목록 (35칸 x 페이지)
 *   슬롯 45–53: 하단 필러
 */
public final class CodexGUI implements CodexOpener {

    public static final String TITLE = "§8[§6아이템 도감§8]";
    public static final int PAGE_SIZE = 36; // slots 9..44

    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;

    public CodexGUI(ItemRegistry itemRegistry, ItemFactory itemFactory) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
    }

    /** Opens the codex at the first page, category = ALL */
    public void open(Player player) {
        open(player, CodexTab.ALL, 0);
    }

    public void open(Player player, CodexTab tab, int page) {
        CodexHolder holder = new CodexHolder(tab, page);
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE);
        holder.setInventory(inv);
        render(inv, tab, page);
        player.openInventory(inv);
    }

    public void render(Inventory inv, CodexTab tab, int page) {
        inv.clear();

        // ── 탭 버튼 ────────────────────────────────────────────────────
        inv.setItem(0, tabButton(Material.BOOK,              "§f전체",   tab == CodexTab.ALL));
        inv.setItem(1, tabButton(Material.IRON_SWORD,        "§b장비",   tab == CodexTab.GEAR));
        inv.setItem(2, tabButton(Material.PAPER,             "§a소모품", tab == CodexTab.CONSUMABLE));
        inv.setItem(3, tabButton(Material.CRYING_OBSIDIAN,   "§9딥마인", tab == CodexTab.DEEPMINE));
        inv.setItem(4, tabButton(Material.NETHER_STAR,       "§5보스",   tab == CodexTab.BOSS));

        // ── 필러 / 룬 도감 / 가이드로 돌아가기 ─────────────────────────
        inv.setItem(5, runeCodexButton());
        ItemStack filler = makeFiller(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 6; i < 8; i++) inv.setItem(i, filler);
        inv.setItem(8, backToGuideButton());
        ItemStack grayFiller = makeFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) inv.setItem(i, grayFiller);

        // ── 아이템 목록 ───────────────────────────────────────────────
        List<ItemDefinition> filtered = filter(itemRegistry.getAll().values(), tab);
        filtered.sort(Comparator.comparingInt(ItemDefinition::getTier).thenComparing(ItemDefinition::getId));

        int totalPages = (int) Math.ceil((double) filtered.size() / PAGE_SIZE);
        int startIdx = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int dataIdx = startIdx + i;
            int slot = 9 + i;
            if (dataIdx >= filtered.size()) break;

            ItemDefinition def = filtered.get(dataIdx);
            ItemStack display;
            try {
                display = itemFactory.create(def.getId()).clone();
            } catch (Exception e) {
                display = new ItemStack(def.getMaterial());
            }

            ItemMeta meta = display.getItemMeta();
            if (meta == null) continue;

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§8▶ 출처: §7" + resolveSource(def));
            lore.add("§8▶ 용도: §7" + resolveUsage(def));
            if (def.getGearScore() > 0) {
                lore.add("§8▶ 전투력(GS): §f" + def.getGearScore());
            }
            if (def.getSellPrice() > 0) {
                lore.add("§8▶ 판매가: §6조개 " + def.getSellPrice());
            }
            lore.add("");
            lore.add("§e▶ 클릭: 상세 정보 보기");
            meta.setLore(lore);
            display.setItemMeta(meta);
            inv.setItem(slot, display);
        }

        // ── 페이지 이동 버튼 ─────────────────────────────────────────
        if (page > 0) {
            inv.setItem(45, pageButton(Material.ARROW, "§a◀ 이전 페이지", page - 1));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, pageButton(Material.ARROW, "§a다음 페이지 ▶", page + 1));
        }

        // Page indicator (slot 49)
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§7페이지 " + (page + 1) + " / " + Math.max(1, totalPages));
            pageMeta.setLore(List.of("§7총 아이템 수: §f" + filtered.size() + "개"));
            pageIndicator.setItemMeta(pageMeta);
        }
        inv.setItem(49, pageIndicator);
    }

    private List<ItemDefinition> filter(Collection<ItemDefinition> all, CodexTab tab) {
        List<ItemDefinition> result = new ArrayList<>();
        for (ItemDefinition def : all) {
            if (tab == CodexTab.ALL || matchesTab(def, tab)) {
                result.add(def);
            }
        }
        return result;
    }

    private boolean matchesTab(ItemDefinition def, CodexTab tab) {
        String id = def.getId();
        return switch (tab) {
            case GEAR -> def.getGearScore() > 0;
            case CONSUMABLE -> id.contains("scroll") || id.contains("stone") || id.contains("setter")
                    || id.contains("fragment") || id.contains("token") || id.contains("bell")
                    || id.contains("essence") || id.contains("extractor") || id.contains("crystal");
            case DEEPMINE -> id.startsWith("deepmine_") || id.contains("abyssal") || id.contains("void_crystal");
            case BOSS -> id.contains("nemesis") || id.contains("soul") || id.contains("void_crystal")
                    || id.contains("abyssal_core");
            default -> true;
        };
    }

    private String resolveSource(ItemDefinition def) {
        String id = def.getId();
        if (id.startsWith("t1_") || id.equals("iron_sword_t1")) return "§b상점 구매 또는 몹 드롭";
        if (id.startsWith("t2_")) return "§b상점 구매 또는 정예 몹 드롭";
        if (id.startsWith("t3_")) return "§9딥 마인 심층 보물 상자";
        if (id.startsWith("t4_")) return "§9딥 마인 미니보스 처치";
        if (id.startsWith("t5_")) return "§5보스 처치 드롭";
        if (id.startsWith("deepmine_")) return "§9딥 마인 보물 상자";
        if (id.equals("void_crystal")) return "§5공허의 기사 처치";
        if (id.equals("abyssal_core")) return "§5딥 마인 미니보스 처치";
        if (id.equals("soul_fragment")) return "§7정예 몹 처치 (희귀)";
        if (id.contains("rune_")) return "§b상점 구매 / 보물 상자";
        if (id.equals("reinforce_stone")) return "§b상점 구매 / 딥 마인 보물 상자";
        if (id.contains("scroll")) return "§b상점 구매";
        if (id.equals("bioluminescent_essence")) return "§a발광 이끼 채집 (tide_extractor 필요)";
        if (id.equals("tide_extractor")) return "§a낚시 또는 상점";
        if (id.equals("nemesis_token")) return "§4네메시스 처치 보상";
        return "§7미분류";
    }

    private String resolveUsage(ItemDefinition def) {
        String id = def.getId();
        if (id.equals("reinforce_stone")) return "대장간에서 장비 강화 재료";
        if (id.equals("protection_scroll")) return "강화 실패 시 파괴 방지";
        if (id.equals("scroll_of_return")) return "사용 시 스폰 지점으로 귀환";
        if (id.equals("spawn_setter")) return "현재 위치를 스폰 지점으로 설정";
        if (id.equals("soul_fragment")) return "보스 제단 소환 재료 (5개)";
        if (id.equals("void_crystal")) return "보스 처치 기념 아이템 / 상점 판매";
        if (id.equals("abyssal_core")) return "T3+ 장비 강화 보조재 / 상점 판매";
        if (id.startsWith("deepmine_")) return "딥 마인 탐험 보조 장비";
        if (id.contains("rune_")) return "대장간에서 장비 소켓에 장착";
        if (id.equals("bioluminescent_essence")) return "상점 판매 또는 제작 재료";
        if (def.getGearScore() > 0) return "장착하여 전투력 향상";
        return "인벤토리 보유 또는 판매";
    }

    private ItemStack tabButton(Material mat, String name, boolean active) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(active ? name + " §a✔" : name);
            if (active) meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack pageButton(Material mat, String name, int targetPage) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of("§7페이지 " + (targetPage + 1) + "로 이동"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack runeCodexButton() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§l🔮 룬 도감");
            meta.setLore(List.of("§7등록된 모든 룬의 종류와", "§7전투 효과를 확인합니다.", "§e▶ 클릭하여 열기"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack backToGuideButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c« 가이드로 돌아가기");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeFiller(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
}
