package com.tide.rpg.shop;

/**
 * One catalog row. Display position is no longer pinned by a manual slot
 * number — ShopGUI auto-flows entries (grouped by kind) across pages, so the
 * catalog can grow indefinitely without ever needing to renumber slots.
 */
public record ShopEntry(String itemId, long price, Currency currency, Kind kind) {

    public ShopEntry(String itemId, long price, Currency currency) {
        this(itemId, price, currency, Kind.ITEM);
    }

    public enum Currency {
        CLAM, PEARL
    }

    public enum Kind {
        ITEM, RUNE, VANILLA
    }
}
