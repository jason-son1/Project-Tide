package com.tide.core.economy;

public enum RepTier {
    BRONZE(0),
    SILVER(200),
    GOLD(500),
    TIDE_MASTER(1000);

    private final int minRep;

    RepTier(int minRep) {
        this.minRep = minRep;
    }

    public int getMinRep() {
        return minRep;
    }

    public static RepTier fromRep(int rep) {
        RepTier result = BRONZE;
        for (RepTier tier : values()) {
            if (rep >= tier.minRep) {
                result = tier;
            }
        }
        return result;
    }
}
