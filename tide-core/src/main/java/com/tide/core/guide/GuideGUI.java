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
 * Three-level in-game tutorial UI: category overview -> entry list -> a
 * Written Book opened via player.openBook() for the full multi-page text,
 * matching Minecraft's native book reader rather than cramming prose into
 * item lore.
 */
public final class GuideGUI {

    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private final GuideRegistry guideRegistry;

    public GuideGUI(GuideRegistry guideRegistry) {
        this.guideRegistry = guideRegistry;
    }

    public void openCategories(Player player) {
        CategoryHolder holder = new CategoryHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, "📖 The Tide — 가이드");
        holder.inventory = inventory;

        ItemStack border = decorative();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        List<GuideCategory> categories = new ArrayList<>(List.of(GuideCategory.values()));
        holder.categories = categories;
        for (int i = 0; i < categories.size() && i < CATEGORY_SLOTS.length; i++) {
            GuideCategory category = categories.get(i);
            int count = guideRegistry.countByCategory(category);
            inventory.setItem(CATEGORY_SLOTS[i], createItem(category.getIcon(),
                    "§e§l" + category.getDisplayName(),
                    "§7",
                    "§7" + category.getDescription(),
                    "§f",
                    "§7항목 수: §f" + count + "개",
                    "§e▶ 클릭하여 목록 보기"
            ));
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    public void openEntries(Player player, GuideCategory category) {
        EntryHolder holder = new EntryHolder();
        holder.category = category;
        Inventory inventory = Bukkit.createInventory(holder, 54, "📖 " + category.getDisplayName());
        holder.inventory = inventory;

        ItemStack border = decorative();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        inventory.setItem(45, createItem(Material.ARROW, "§c« 돌아가기", "§7클릭하여 카테고리 목록으로 돌아갑니다."));

        List<GuideEntry> entries = guideRegistry.byCategory(category);
        holder.entries = entries;
        int slot = 9;
        for (GuideEntry entry : entries) {
            if (slot >= 45) {
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

    public static final class CategoryHolder implements InventoryHolder {
        private Inventory inventory;
        private List<GuideCategory> categories = List.of();

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public List<GuideCategory> getCategories() {
            return categories;
        }
    }

    public static final class EntryHolder implements InventoryHolder {
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
