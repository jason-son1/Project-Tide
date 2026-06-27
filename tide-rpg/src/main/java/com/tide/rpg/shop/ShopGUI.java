package com.tide.rpg.shop;

import com.tide.rpg.item.ItemFactory;
import com.tide.rpg.rune.RuneItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paginated shop renderer. Entries are grouped by kind (커스텀/룬 first, then
 * 바닐라) and auto-flowed across pages of {@link #PAGE_SIZE} content slots —
 * the catalog can grow without ever needing manual slot bookkeeping; once a
 * page fills up, a "다음 페이지" arrow simply appears.
 */
public final class ShopGUI {

    private static final int PREV_BUTTON_SLOT = 3;
    private static final int PAGE_INDICATOR_SLOT = 4;
    private static final int NEXT_BUTTON_SLOT = 5;
    private static final int GUIDE_BUTTON_SLOT = 8;
    private static final int CONTENT_START_SLOT = 9;
    private static final int CONTENT_END_SLOT = 53; // inclusive
    private static final int PAGE_SIZE = CONTENT_END_SLOT - CONTENT_START_SLOT + 1;

    private final ShopConfig shopConfig;
    private final ItemFactory itemFactory;
    private final RuneItemFactory runeItemFactory;

    public ShopGUI(ShopConfig shopConfig, ItemFactory itemFactory, RuneItemFactory runeItemFactory) {
        this.shopConfig = shopConfig;
        this.itemFactory = itemFactory;
        this.runeItemFactory = runeItemFactory;
    }

    /** Hot-reloads the catalog from shop.yml; kept for callers that still reference it directly. */
    public void loadConfig() {
        shopConfig.reload();
    }

    public void open(Player player) {
        ShopHolder holder = new ShopHolder(shopConfig.getBuyEntries(), shopConfig.getSellEntries());
        Inventory inventory = Bukkit.createInventory(holder, 54, "§6The Tide 상점");
        holder.setInventory(inventory);
        render(inventory, ShopTab.BUY, player);
        player.openInventory(inventory);
    }

    public void render(Inventory inventory, ShopTab tab, Player player) {
        if (!(inventory.getHolder() instanceof ShopHolder holder)) {
            return;
        }
        inventory.clear();

        inventory.setItem(0, tabButton(Material.GOLD_INGOT, "§a🛒 아이템 구매", tab == ShopTab.BUY));
        inventory.setItem(1, tabButton(Material.CHEST, "§6💰 아이템 판매", tab == ShopTab.SELL));

        List<ShopEntry> currentEntries = (tab == ShopTab.BUY) ? holder.getBuyEntries() : holder.getSellEntries();
        List<Row> rows = buildRows(currentEntries);
        int totalPages = Math.max(1, (int) Math.ceil(rows.size() / (double) PAGE_SIZE));
        int page = Math.max(0, Math.min(holder.getPage(tab), totalPages - 1));
        holder.setPage(tab, page);

        ItemStack filler = marker(Material.GRAY_STAINED_GLASS_PANE, " ");
        inventory.setItem(2, filler);
        inventory.setItem(6, filler);
        inventory.setItem(7, filler);
        inventory.setItem(PREV_BUTTON_SLOT, page > 0
                ? marker(Material.ARROW, "§e◀ 이전 페이지")
                : filler);
        inventory.setItem(PAGE_INDICATOR_SLOT, marker(Material.PAPER, "§f페이지 " + (page + 1) + " / " + totalPages,
                "§7항목 " + rows.stream().filter(r -> r instanceof EntryRow).count() + "개"));
        inventory.setItem(NEXT_BUTTON_SLOT, page < totalPages - 1
                ? marker(Material.ARROW, "§e다음 페이지 ▶")
                : filler);
        inventory.setItem(GUIDE_BUTTON_SLOT, marker(Material.WRITTEN_BOOK, "§a📖 상점 가이드 보기",
                "§7클릭하면 상점 및 경제 가이드를 엽니다."));

        Map<Integer, ShopEntry> slotMap = new HashMap<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(rows.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            int slot = CONTENT_START_SLOT + (i - start);
            Row row = rows.get(i);
            if (row instanceof HeaderRow header) {
                inventory.setItem(slot, sectionHeader(header.material(), header.title(), header.lore()));
            } else if (row instanceof EntryRow entryRow) {
                ItemStack display = buildEntryDisplay(entryRow.entry(), tab, player);
                if (display != null) {
                    inventory.setItem(slot, display);
                    slotMap.put(slot, entryRow.entry());
                }
            }
        }
        holder.setSlotMap(tab, slotMap);

        for (int slot = CONTENT_START_SLOT; slot <= CONTENT_END_SLOT; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    public void changePage(Inventory inventory, ShopHolder holder, Player player, int delta) {
        ShopTab tab = holder.getTab();
        holder.setPage(tab, holder.getPage(tab) + delta);
        render(inventory, tab, player);
    }

    // ---------- row model ----------

    private sealed interface Row {}

    private record HeaderRow(Material material, String title, String... lore) implements Row {}

    private record EntryRow(ShopEntry entry) implements Row {}

    private List<Row> buildRows(List<ShopEntry> entries) {
        List<Row> rows = new ArrayList<>();
        List<ShopEntry> custom = entries.stream().filter(e -> e.kind() != ShopEntry.Kind.VANILLA).toList();
        List<ShopEntry> vanilla = entries.stream().filter(e -> e.kind() == ShopEntry.Kind.VANILLA).toList();
        if (!custom.isEmpty()) {
            rows.add(new HeaderRow(Material.NAME_TAG, "§b▶ 커스텀 아이템 / 룬",
                    "§7The Tide 전용 장비, 소모품, 룬 목록입니다."));
            custom.forEach(e -> rows.add(new EntryRow(e)));
        }
        if (!vanilla.isEmpty()) {
            rows.add(new HeaderRow(Material.GRASS_BLOCK, "§a▶ 바닐라 원자재",
                    "§7일반 마인크래프트 아이템을 조개로 환전합니다."));
            vanilla.forEach(e -> rows.add(new EntryRow(e)));
        }
        return rows;
    }

    private ItemStack buildEntryDisplay(ShopEntry entry, ShopTab tab, Player player) {
        ItemStack display = createDisplayItem(entry);
        if (display == null) {
            return null;
        }
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            return display;
        }

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        String priceLabel = entry.currency() == ShopEntry.Currency.CLAM
                ? "§6조개 " + entry.price() + "개"
                : "§d진주 " + entry.price() + "개";

        if (tab == ShopTab.BUY) {
            lore.add("§a구매 가격: " + priceLabel);
            lore.add("§e▶ 클릭하여 구매");
        } else {
            lore.add("§6판매 가격: " + priceLabel);
            int count = countInventoryItems(player, entry);
            lore.add("§7보유 수량: §f" + count + "개");
            lore.add("§e▶ 좌클릭: 1개 판매");
            lore.add("§e▶ 우클릭: 모두 판매");
        }

        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack sectionHeader(Material material, String name, String... lore) {
        return marker(material, name, lore);
    }

    private ItemStack marker(Material material, String name, String... lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack tabButton(Material material, String name, boolean active) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(active ? name + " §a✔" : name);
            if (active) {
                meta.setEnchantmentGlintOverride(true);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack createDisplayItem(ShopEntry entry) {
        try {
            if (entry.kind() == ShopEntry.Kind.RUNE) {
                return runeItemFactory.create(entry.itemId());
            } else if (entry.kind() == ShopEntry.Kind.ITEM) {
                return itemFactory.create(entry.itemId());
            } else {
                Material material = Material.matchMaterial(entry.itemId());
                if (material == null) {
                    material = Material.STONE;
                }
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§f" + translateVanillaName(material));
                    item.setItemMeta(meta);
                }
                return item;
            }
        } catch (Exception exception) {
            return new ItemStack(Material.BARRIER);
        }
    }

    private String translateVanillaName(Material material) {
        return switch (material) {
            case COAL -> "석탄";
            case RAW_COPPER -> "원목 구리";
            case RAW_IRON -> "철 광석";
            case IRON_INGOT -> "철 주괴";
            case RAW_GOLD -> "금 광석";
            case GOLD_INGOT -> "금 주괴";
            case REDSTONE -> "레드스톤";
            case LAPIS_LAZULI -> "청금석";
            case DIAMOND -> "다이아몬드";
            case EMERALD -> "에메랄드";
            case QUARTZ -> "네더 석영";
            case AMETHYST_SHARD -> "자수정 조각";
            case ENDER_PEARL -> "엔더 진주";
            case BLAZE_ROD -> "블레이즈 막대";
            case GHAST_TEAR -> "가스트의 눈물";
            case PHANTOM_MEMBRANE -> "팬텀 막";
            case SHULKER_SHELL -> "셜커의 껍질";
            default -> material.name();
        };
    }

    public int countInventoryItems(Player player, ShopEntry entry) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (matches(item, entry)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public boolean matches(ItemStack item, ShopEntry entry) {
        if (entry.kind() == ShopEntry.Kind.RUNE) {
            String runeId = runeItemFactory.readRuneId(item);
            return entry.itemId().equals(runeId);
        } else if (entry.kind() == ShopEntry.Kind.ITEM) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            }
            String itemId = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey("tide", "item_id"), org.bukkit.persistence.PersistentDataType.STRING);
            return entry.itemId().equals(itemId);
        } else {
            Material material = Material.matchMaterial(entry.itemId());
            if (item.getType() != material) {
                return false;
            }
            if (item.hasItemMeta()) {
                var pdc = item.getItemMeta().getPersistentDataContainer();
                if (pdc.has(new org.bukkit.NamespacedKey("tide", "item_id"), org.bukkit.persistence.PersistentDataType.STRING) ||
                    pdc.has(new org.bukkit.NamespacedKey("tide", "rune_id"), org.bukkit.persistence.PersistentDataType.STRING)) {
                    return false;
                }
            }
            return true;
        }
    }
}
