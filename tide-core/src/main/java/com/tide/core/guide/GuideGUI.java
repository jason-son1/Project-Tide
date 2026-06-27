package com.tide.core.guide;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-screen tutorial UI: category tabs (top row) switch the entry list
 * in-place, and an "아이템 도감" tab opens the item codex (tide-rpg, via the
 * {@link CodexOpener} service) — collapsing what used to be a 3-step
 * category-list -> entry-list -> book flow into 2 steps (open menu -> click
 * entry -> book), with the item codex one click away from the same screen.
 */
public final class GuideGUI {

    public static final int[] CATEGORY_TAB_SLOTS = {0, 1, 2, 3, 4, 5, 6};
    public static final int CODEX_TAB_SLOT = 7;
    public static final int ENTRY_START_SLOT = 9;
    public static final int ENTRY_END_SLOT = 44; // inclusive

    private final GuideRegistry guideRegistry;

    public GuideGUI(GuideRegistry guideRegistry) {
        this.guideRegistry = guideRegistry;
    }

    /** Opens the guide on the first category. Kept for existing callers (e.g. /guide). */
    public void openCategories(Player player) {
        open(player, GuideCategory.values()[0]);
    }

    /** Opens the guide pre-selected on the given category. Kept for existing callers
     *  (ShopListener/ForgeListener's in-context "가이드 보기" buttons). */
    public void openEntries(Player player, GuideCategory category) {
        open(player, category);
    }

    private void open(Player player, GuideCategory category) {
        GuideHolder holder = new GuideHolder();
        holder.category = category;
        Inventory inventory = Bukkit.createInventory(holder, 54, "📖 The Tide — 서버 가이드");
        holder.inventory = inventory;

        ItemStack border = decorative();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }
        inventory.setItem(8, border);

        GuideCategory[] categories = GuideCategory.values();
        for (int i = 0; i < categories.length && i < CATEGORY_TAB_SLOTS.length; i++) {
            GuideCategory c = categories[i];
            boolean active = c == category;
            int count = guideRegistry.countByCategory(c);
            inventory.setItem(CATEGORY_TAB_SLOTS[i], createItem(c.getIcon(),
                    (active ? "§a§l✔ " : "§e§l") + c.getDisplayName(),
                    "§7" + c.getDescription(),
                    "§7항목 수: §f" + count + "개",
                    active ? "§a현재 선택됨" : "§e▶ 클릭하여 보기"
            ));
        }
        for (int i = categories.length; i < CATEGORY_TAB_SLOTS.length; i++) {
            inventory.setItem(CATEGORY_TAB_SLOTS[i], border);
        }
        inventory.setItem(CODEX_TAB_SLOT, createItem(Material.WRITTEN_BOOK,
                "§b§l📚 아이템 도감",
                "§7등록된 모든 커스텀 아이템의",
                "§7출처와 용도를 확인합니다.",
                "§e▶ 클릭하여 열기"
        ));

        List<GuideEntry> entries = guideRegistry.byCategory(category);
        holder.entries = entries;
        int slot = ENTRY_START_SLOT;
        for (GuideEntry entry : entries) {
            if (slot > ENTRY_END_SLOT) {
                break;
            }
            List<String> lore = new ArrayList<>();
            lore.add("§7");
            lore.add("§f" + entry.getSummary());
            lore.add("§7");
            if (!entry.getCommands().isEmpty()) {
                lore.add("§7관련 명령어: §f" + String.join(", ", entry.getCommands()));
            }
            lore.add("§e▶ 클릭하여 자세히 보기");
            inventory.setItem(slot, createItem(entry.getIcon(), "§b§l" + entry.getTitle(), lore.toArray(new String[0])));
            slot++;
        }
        for (int s = slot; s <= ENTRY_END_SLOT; s++) {
            inventory.setItem(s, border);
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    public void openBook(Player player, GuideEntry entry) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle(entry.getTitle());
            meta.setAuthor("The Tide");
            meta.setPages(entry.getPages());
            book.setItemMeta(meta);
        }
        player.openBook(book);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.2f);
    }

    private ItemStack decorative() {
        ItemStack item = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final class GuideHolder implements InventoryHolder {
        private Inventory inventory;
        private GuideCategory category;
        private List<GuideEntry> entries = List.of();

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public GuideCategory getCategory() {
            return category;
        }

        public List<GuideEntry> getEntries() {
            return entries;
        }
    }
}
