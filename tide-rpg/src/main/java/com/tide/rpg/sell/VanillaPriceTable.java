package com.tide.rpg.sell;

import org.bukkit.Material;

import java.util.Map;

/** Sell prices (in clam) for plain vanilla junk — used by /sellall when an item has no Tide item_id. */
public final class VanillaPriceTable {

    private static final Map<Material, Integer> PRICES = Map.ofEntries(
            Map.entry(Material.ROTTEN_FLESH, 1),
            Map.entry(Material.BONE, 2),
            Map.entry(Material.STRING, 2),
            Map.entry(Material.SPIDER_EYE, 3),
            Map.entry(Material.GUNPOWDER, 3),
            Map.entry(Material.RABBIT_HIDE, 2),
            Map.entry(Material.FEATHER, 2),
            Map.entry(Material.LEATHER, 4),
            Map.entry(Material.INK_SAC, 3),
            Map.entry(Material.SLIME_BALL, 4),
            Map.entry(Material.PHANTOM_MEMBRANE, 10),
            Map.entry(Material.COAL, 5),
            Map.entry(Material.IRON_INGOT, 12),
            Map.entry(Material.GOLD_INGOT, 18),
            Map.entry(Material.REDSTONE, 3),
            Map.entry(Material.LAPIS_LAZULI, 4),
            Map.entry(Material.QUARTZ, 4),
            Map.entry(Material.PRISMARINE_SHARD, 6),
            Map.entry(Material.PRISMARINE_CRYSTALS, 8)
    );

    private VanillaPriceTable() {
    }

    public static Integer priceOf(Material material) {
        return PRICES.get(material);
    }
}
