package com.tide.rpg.shop;

import com.tide.rpg.item.ItemFactory;
import com.tide.rpg.rune.RuneItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ShopGUI {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final RuneItemFactory runeItemFactory;
    private final List<ShopEntry> buyEntries = new ArrayList<>();
    private final List<ShopEntry> sellEntries = new ArrayList<>();

    public ShopGUI(JavaPlugin plugin, ItemFactory itemFactory, RuneItemFactory runeItemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.runeItemFactory = runeItemFactory;
        loadConfig();
    }

    public void loadConfig() {
        buyEntries.clear();
        sellEntries.clear();

        var config = plugin.getConfig();
        if (config.contains("shop.buy")) {
            for (var rawMap : config.getMapList("shop.buy")) {
                try {
                    String id = (String) rawMap.get("id");
                    long price = ((Number) rawMap.get("price")).longValue();
                    String currencyStr = (String) rawMap.get("currency");
                    ShopEntry.Currency currency = ShopEntry.Currency.valueOf(currencyStr.toUpperCase());
                    int slot = ((Number) rawMap.get("slot")).intValue();
                    String kindStr = (String) rawMap.get("kind");
                    ShopEntry.Kind kind = ShopEntry.Kind.valueOf(kindStr.toUpperCase());

                    buyEntries.add(new ShopEntry(id, price, currency, slot, kind));
                } catch (Exception exception) {
                    plugin.getLogger().warning("Failed to load shop.buy entry: " + rawMap + " - " + exception.getMessage());
                }
            }
        }

        if (config.contains("shop.sell")) {
            for (var rawMap : config.getMapList("shop.sell")) {
                try {
                    String id = (String) rawMap.get("id");
                    long price = ((Number) rawMap.get("price")).longValue();
                    String currencyStr = (String) rawMap.get("currency");
                    ShopEntry.Currency currency = ShopEntry.Currency.valueOf(currencyStr.toUpperCase());
                    int slot = ((Number) rawMap.get("slot")).intValue();
                    String kindStr = (String) rawMap.get("kind");
                    ShopEntry.Kind kind = ShopEntry.Kind.valueOf(kindStr.toUpperCase());

                    sellEntries.add(new ShopEntry(id, price, currency, slot, kind));
                } catch (Exception exception) {
                    plugin.getLogger().warning("Failed to load shop.sell entry: " + rawMap + " - " + exception.getMessage());
                }
            }
        }

        // Setup defaults if empty
        if (buyEntries.isEmpty()) {
            buyEntries.add(new ShopEntry("rune_lifesteal_1", 2, ShopEntry.Currency.PEARL, 10, ShopEntry.Kind.RUNE));
            buyEntries.add(new ShopEntry("rune_lightning_1", 2, ShopEntry.Currency.PEARL, 11, ShopEntry.Kind.RUNE));
            buyEntries.add(new ShopEntry("t1_leather_helmet", 80, ShopEntry.Currency.CLAM, 12, ShopEntry.Kind.ITEM));
            buyEntries.add(new ShopEntry("iron_sword_t1", 200, ShopEntry.Currency.CLAM, 13, ShopEntry.Kind.ITEM));
            buyEntries.add(new ShopEntry("reinforce_stone", 50, ShopEntry.Currency.CLAM, 14, ShopEntry.Kind.ITEM));
            buyEntries.add(new ShopEntry("protection_scroll", 3, ShopEntry.Currency.PEARL, 22, ShopEntry.Kind.ITEM));
        }

        if (sellEntries.isEmpty()) {
            sellEntries.add(new ShopEntry("iron_sword_t1", 60, ShopEntry.Currency.CLAM, 10, ShopEntry.Kind.ITEM));
            sellEntries.add(new ShopEntry("reinforce_stone", 15, ShopEntry.Currency.CLAM, 11, ShopEntry.Kind.ITEM));
            sellEntries.add(new ShopEntry("COAL", 2, ShopEntry.Currency.CLAM, 12, ShopEntry.Kind.VANILLA));
            sellEntries.add(new ShopEntry("RAW_IRON", 5, ShopEntry.Currency.CLAM, 13, ShopEntry.Kind.VANILLA));
            sellEntries.add(new ShopEntry("RAW_GOLD", 10, ShopEntry.Currency.CLAM, 14, ShopEntry.Kind.VANILLA));
            sellEntries.add(new ShopEntry("DIAMOND", 30, ShopEntry.Currency.CLAM, 15, ShopEntry.Kind.VANILLA));
            sellEntries.add(new ShopEntry("EMERALD", 40, ShopEntry.Currency.CLAM, 16, ShopEntry.Kind.VANILLA));
        }
    }

    public void open(Player player) {
        ShopHolder holder = new ShopHolder(buyEntries, sellEntries);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§6The Tide 상점");
        holder.setInventory(inventory);
        render(inventory, ShopTab.BUY, player);
        player.openInventory(inventory);
    }

    public void render(Inventory inventory, ShopTab tab, Player player) {
        inventory.clear();

        // Render tab buttons
        inventory.setItem(0, tabButton(Material.GOLD_INGOT, "§a🛒 아이템 구매", tab == ShopTab.BUY));
        inventory.setItem(1, tabButton(Material.CHEST, "§6💰 아이템 판매", tab == ShopTab.SELL));

        ItemStack headerFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta headerFillerMeta = headerFiller.getItemMeta();
        if (headerFillerMeta != null) {
            headerFillerMeta.setDisplayName(" ");
            headerFiller.setItemMeta(headerFillerMeta);
        }
        for (int i = 2; i < 8; i++) {
            inventory.setItem(i, headerFiller);
        }
        ItemStack guideBook = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta guideMeta = guideBook.getItemMeta();
        if (guideMeta != null) {
            guideMeta.setDisplayName("§a📖 상점 가이드 보기");
            guideMeta.setLore(List.of("§7클릭하면 상점 및 경제 가이드를 엽니다."));
            guideBook.setItemMeta(guideMeta);
        }
        inventory.setItem(8, guideBook);

        List<ShopEntry> currentEntries = (tab == ShopTab.BUY) ? buyEntries : sellEntries;

        for (ShopEntry entry : currentEntries) {
            ItemStack display = createDisplayItem(entry);
            if (display == null) {
                continue;
            }

            ItemMeta meta = display.getItemMeta();
            if (meta == null) {
                continue;
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
            inventory.setItem(entry.slot(), display);
        }

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int slot = 9; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
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
            case RAW_IRON -> "철 광석";
            case RAW_GOLD -> "금 광석";
            case DIAMOND -> "다이아몬드";
            case EMERALD -> "에메랄드";
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
