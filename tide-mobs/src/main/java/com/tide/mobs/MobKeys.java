package com.tide.mobs;

import org.bukkit.NamespacedKey;

/** Additional PDC keys beyond the shared tide:affixes / tide:elite table, scoped to mob behavior. */
public final class MobKeys {

    private MobKeys() {
    }

    public static final NamespacedKey DAMAGE_MULTIPLIER = new NamespacedKey("tide", "damage_multiplier");
    public static final NamespacedKey BOSS_MARKER = new NamespacedKey("tide", "boss_altar_id");
    public static final NamespacedKey ELITE = new NamespacedKey("tide", "elite");
    public static final NamespacedKey AFFIXES = new NamespacedKey("tide", "affixes");
    public static final NamespacedKey CALAMITY = new NamespacedKey("tide", "calamity");
    public static final NamespacedKey CALAMITY_ESCORT = new NamespacedKey("tide", "calamity_escort_of");
    public static final NamespacedKey STOLEN_FROM = new NamespacedKey("tide", "stolen_from");
}
