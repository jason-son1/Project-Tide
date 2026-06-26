package com.tide.core.tide;

public enum TideState {
    HIGH_TIDE("§b🌊 밀물"),
    LOW_TIDE("§9💨 썰물"),
    SPRING_TIDE("§e🌟 사리"),
    BLOOD_MOON("§c🩸 블러드문"),
    BLOOD_TIDE("§4🩸🌟 블러드 사리");

    private final String displayName;

    TideState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
