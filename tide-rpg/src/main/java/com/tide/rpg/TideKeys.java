package com.tide.rpg;

import org.bukkit.NamespacedKey;

/**
 * Shared PDC namespace ("tide") used by TideRPG and TideMobs. A raw
 * NamespacedKey(namespace, key) is used instead of a plugin-bound key so
 * both jars read/write the exact same key without depending on each
 * other's plugin instance.
 */
public final class TideKeys {

    private TideKeys() {
    }

    public static final String NAMESPACE = "tide";

    public static final NamespacedKey ITEM_ID = new NamespacedKey(NAMESPACE, "item_id");
    public static final NamespacedKey GS = new NamespacedKey(NAMESPACE, "gs");
    public static final NamespacedKey REINFORCE = new NamespacedKey(NAMESPACE, "reinforce");
    public static final NamespacedKey SOCKET_COUNT = new NamespacedKey(NAMESPACE, "socket_count");
    public static final NamespacedKey SOCKET_1 = new NamespacedKey(NAMESPACE, "socket_1");
    public static final NamespacedKey SOCKET_2 = new NamespacedKey(NAMESPACE, "socket_2");
    public static final NamespacedKey SOCKET_3 = new NamespacedKey(NAMESPACE, "socket_3");
    public static final NamespacedKey CMD = new NamespacedKey(NAMESPACE, "cmd");
    public static final NamespacedKey RUNE_ID = new NamespacedKey(NAMESPACE, "rune_id");

    public static final NamespacedKey AFFIXES = new NamespacedKey(NAMESPACE, "affixes");
    public static final NamespacedKey ELITE = new NamespacedKey(NAMESPACE, "elite");
    public static final NamespacedKey NEMESIS_TARGET = new NamespacedKey(NAMESPACE, "nemesis_target");
    public static final NamespacedKey GRAVE_OWNER = new NamespacedKey(NAMESPACE, "grave_owner");
    public static final NamespacedKey GRAVE_ID = new NamespacedKey(NAMESPACE, "grave_id");
    public static final NamespacedKey ZONE_ID = new NamespacedKey(NAMESPACE, "zone_id");

    // 심화 확장 기능 백서 (추가기능1) 6장 PDC 스키마
    public static final NamespacedKey RESONANCE = new NamespacedKey(NAMESPACE, "resonance");
    public static final NamespacedKey ANCHOR = new NamespacedKey(NAMESPACE, "anchor");
    public static final NamespacedKey OXYGEN_CAPACITY = new NamespacedKey(NAMESPACE, "oxygen_capacity");
    public static final NamespacedKey HEAVY_CARGO = new NamespacedKey(NAMESPACE, "heavy_cargo");
    public static final NamespacedKey STOLEN_FROM = new NamespacedKey(NAMESPACE, "stolen_from");

    public static NamespacedKey socket(int index) {
        return switch (index) {
            case 1 -> SOCKET_1;
            case 2 -> SOCKET_2;
            case 3 -> SOCKET_3;
            default -> throw new IllegalArgumentException("Socket index must be 1-3: " + index);
        };
    }
}
