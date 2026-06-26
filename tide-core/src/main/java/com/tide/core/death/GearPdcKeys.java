package com.tide.core.death;

import org.bukkit.NamespacedKey;

/**
 * Duplicates TideRPG's "tide:reinforce" key by value (not by class reference —
 * TideCore must not depend on TideRPG) so the hard-mode death penalty can
 * read/write the same PDC entry on equipped gear.
 */
public final class GearPdcKeys {

    private GearPdcKeys() {
    }

    public static final NamespacedKey REINFORCE = new NamespacedKey("tide", "reinforce");
}
