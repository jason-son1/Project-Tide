package com.tide.core.guide;

import org.bukkit.entity.Player;

/**
 * Service interface so GuideGUI (tide-core) can open the item codex
 * (implemented in tide-rpg) without tide-core depending on tide-rpg.
 * Registered via Bukkit's ServicesManager — same pattern as ItemFactory/
 * GearScoreCalculator.
 */
public interface CodexOpener {

    void open(Player player);
}
