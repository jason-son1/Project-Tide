package com.tide.rpg.shop;

public record ShopEntry(String itemId, long price, Currency currency, int slot) {

    public enum Currency {
        CLAM, PEARL
    }
}
