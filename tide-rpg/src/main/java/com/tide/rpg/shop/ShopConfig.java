package com.tide.rpg.shop;

import com.tide.core.reload.Reloadable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated config file for the shop (상점) system — kept separate from the
 * plugin's main config.yml so the buy/sell catalog can be hand-edited and
 * hot-reloaded (/tide reload shop) independently of unrelated settings.
 */
public final class ShopConfig implements Reloadable {

    private final JavaPlugin plugin;
    private final File file;
    private List<ShopEntry> buyEntries = new ArrayList<>();
    private List<ShopEntry> sellEntries = new ArrayList<>();

    public ShopConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        reload();
    }

    @Override
    public int reload() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ShopEntry> loadedBuy = new ArrayList<>();
        List<ShopEntry> loadedSell = new ArrayList<>();
        parseList(config, "buy", loadedBuy);
        parseList(config, "sell", loadedSell);
        this.buyEntries = loadedBuy.isEmpty() ? defaultBuy() : loadedBuy;
        this.sellEntries = loadedSell.isEmpty() ? defaultSell() : loadedSell;
        plugin.getLogger().info("상점 설정 로드 완료: 구매 " + buyEntries.size() + "건, 판매 " + sellEntries.size() + "건");
        return buyEntries.size() + sellEntries.size();
    }

    private void parseList(YamlConfiguration config, String key, List<ShopEntry> target) {
        if (!config.contains(key)) {
            return;
        }
        for (var rawMap : config.getMapList(key)) {
            try {
                String id = (String) rawMap.get("id");
                long price = ((Number) rawMap.get("price")).longValue();
                ShopEntry.Currency currency = ShopEntry.Currency.valueOf(String.valueOf(rawMap.get("currency")).toUpperCase());
                Object kindRaw = rawMap.containsKey("kind") ? rawMap.get("kind") : "ITEM";
                ShopEntry.Kind kind = ShopEntry.Kind.valueOf(String.valueOf(kindRaw).toUpperCase());
                target.add(new ShopEntry(id, price, currency, kind));
            } catch (Exception exception) {
                plugin.getLogger().warning("상점 항목 로드 실패(" + key + "): " + rawMap + " - " + exception.getMessage());
            }
        }
    }

    private List<ShopEntry> defaultBuy() {
        return List.of(
                new ShopEntry("reinforce_stone", 50, ShopEntry.Currency.CLAM, ShopEntry.Kind.ITEM),
                new ShopEntry("protection_scroll", 5, ShopEntry.Currency.PEARL, ShopEntry.Kind.ITEM),
                new ShopEntry("iron_sword_t1", 200, ShopEntry.Currency.CLAM, ShopEntry.Kind.ITEM)
        );
    }

    private List<ShopEntry> defaultSell() {
        return List.of(
                new ShopEntry("COAL", 2, ShopEntry.Currency.CLAM, ShopEntry.Kind.VANILLA),
                new ShopEntry("IRON_INGOT", 8, ShopEntry.Currency.CLAM, ShopEntry.Kind.VANILLA)
        );
    }

    public List<ShopEntry> getBuyEntries() {
        return buyEntries;
    }

    public List<ShopEntry> getSellEntries() {
        return sellEntries;
    }
}
