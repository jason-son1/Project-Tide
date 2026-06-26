package com.tide.rpg.tideext;

import com.tide.rpg.TideKeys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player Tidal Overdrive gauge (2-2) — purely in-memory, resets on logout. */
public final class OverdriveManager {

    private static final double MAX_GAUGE = 100.0;
    private static final double GAUGE_PER_DAMAGE = 2.0;

    private final Map<UUID, Double> gauge = new ConcurrentHashMap<>();

    public boolean isWearingResonance(Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (hasResonance(weapon)) {
            return true;
        }
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (hasResonance(armorPiece)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasResonance(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return false;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().has(TideKeys.RESONANCE, PersistentDataType.STRING);
    }

    public void addGauge(Player player, double damageDealt) {
        double current = gauge.getOrDefault(player.getUniqueId(), 0.0);
        double updated = Math.min(MAX_GAUGE, current + damageDealt * GAUGE_PER_DAMAGE);
        gauge.put(player.getUniqueId(), updated);
    }

    public double getGauge(Player player) {
        return gauge.getOrDefault(player.getUniqueId(), 0.0);
    }

    public boolean isFull(Player player) {
        return getGauge(player) >= MAX_GAUGE;
    }

    public void consume(Player player) {
        gauge.put(player.getUniqueId(), 0.0);
    }
}
