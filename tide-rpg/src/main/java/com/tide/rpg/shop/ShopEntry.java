package com.tide.rpg.shop;

public record ShopEntry(String itemId, long price, Currency currency, int slot, Kind kind) {

    public ShopEntry(String itemId, long price, Currency currency, int slot) {
        this(itemId, price, currency, slot, Kind.ITEM);
    }

    public enum Currency {
        CLAM, PEARL
    }

    public enum Kind {
        ITEM, RUNE, VANILLA
    }
}
