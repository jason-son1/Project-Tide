package com.tide.core.difficulty;

import com.tide.core.economy.EconomyAPI;
import com.tide.core.tide.TideState;
import com.tide.core.tide.TideStateProvider;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Central difficulty-scaling brain (see Spec Sheet: Dynamic World Difficulty
 * Scaling System). Reads everything from TideCore's config.yml
 * ("difficulty-scaling" node) on every call — no caching beyond what
 * YamlConfiguration already does in memory — so /tide reload config picks
 * up tuning changes immediately. Registered as a Bukkit service so TideRPG
 * (shop sell bonus) and TideMobs (spawn scaling) can consume it without a
 * compile-time dependency on TideCore's plugin class.
 */
public final class DifficultyManager {

    private static final DifficultyBracket FALLBACK_BRACKET =
            new DifficultyBracket("t1", 0, 500, 1.3, 1.15, 1, 1.0);

    private final JavaPlugin plugin;
    private final EconomyAPI economyAPI;
    private final TideStateProvider tideStateProvider;

    public DifficultyManager(JavaPlugin plugin, EconomyAPI economyAPI, TideStateProvider tideStateProvider) {
        this.plugin = plugin;
        this.economyAPI = economyAPI;
        this.tideStateProvider = tideStateProvider;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("difficulty-scaling.enabled", true);
    }

    /** All configured brackets, sorted by min-pi ascending. */
    public List<DifficultyBracket> brackets() {
        List<DifficultyBracket> result = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("difficulty-scaling.brackets");
        if (section == null) {
            result.add(FALLBACK_BRACKET);
            return result;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection b = section.getConfigurationSection(key);
            if (b == null) {
                continue;
            }
            result.add(new DifficultyBracket(
                    key,
                    b.getDouble("min-pi", 0),
                    b.getDouble("max-pi", 0),
                    b.getDouble("hp-scale", 1.0),
                    b.getDouble("dmg-scale", 1.0),
                    b.getInt("max-affixes", 1),
                    b.getDouble("sell-bonus", 1.0)
            ));
        }
        if (result.isEmpty()) {
            result.add(FALLBACK_BRACKET);
        }
        result.sort((a, c) -> Double.compare(a.minPi(), c.minPi()));
        return result;
    }

    public DifficultyBracket bracketFor(double pi) {
        List<DifficultyBracket> brackets = brackets();
        for (DifficultyBracket bracket : brackets) {
            if (bracket.contains(pi)) {
                return bracket;
            }
        }
        // Below the lowest bracket's min, or above the highest bracket's max.
        DifficultyBracket last = brackets.get(brackets.size() - 1);
        return pi > last.maxPi() ? last : brackets.get(0);
    }

    /**
     * Multiplayer-corrected Effective PI for everyone within scan-radius of a
     * location: alpha*max + (1-alpha)*average, so a veteran carrying a newbie
     * doesn't flatten to the average (too easy) or spike to the max (newbie
     * gets one-shot).
     */
    public double effectivePI(Location location) {
        if (location.getWorld() == null) {
            return 0;
        }
        double radius = plugin.getConfig().getDouble("difficulty-scaling.scan-radius", 48);
        double radiusSq = radius * radius;
        List<Double> pis = new ArrayList<>();
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSq) {
                pis.add(economyAPI.getProgressionIndex(player.getUniqueId()));
            }
        }
        if (pis.isEmpty()) {
            return 0;
        }
        double max = pis.get(0);
        double sum = 0;
        for (double pi : pis) {
            max = Math.max(max, pi);
            sum += pi;
        }
        double average = sum / pis.size();
        double alpha = plugin.getConfig().getDouble("difficulty-scaling.multiplayer-alpha", 0.6);
        return alpha * max + (1 - alpha) * average;
    }

    public double tideMultiplier(TideState state) {
        return plugin.getConfig().getDouble("difficulty-scaling.tide-multipliers." + state.name(), 1.0);
    }

    /**
     * Full resolution for one spawn/encounter. HP/damage multipliers are linearly
     * interpolated across the brackets' (max-pi, *-scale) control points — starting
     * from (0, 1.0x) — instead of multiplying a separate continuous curve by the
     * bracket's step multiplier. The old formula stacked both in the same direction
     * (curve grows with PI, then the bracket scale ALSO grows with PI), which produced
     * a sharp cliff at every bracket boundary and runaway totals at high PI (e.g. T5
     * during Blood Tide could reach ~40x base HP). Interpolating means growth is
     * smooth and bounded by the bracket table itself — one place to tune the curve.
     * TideState still multiplies on top as a deliberate, temporary event spike.
     */
    public DifficultyResult resolve(Location location) {
        double pi = effectivePI(location);
        List<DifficultyBracket> sorted = brackets();
        DifficultyBracket bracket = bracketFor(pi);
        double tideMult = tideMultiplier(tideStateProvider.getCurrentState());
        double hpMultiplier = interpolate(sorted, pi, true) * tideMult;
        double dmgMultiplier = interpolate(sorted, pi, false) * tideMult;
        return new DifficultyResult(bracket, pi, hpMultiplier, dmgMultiplier);
    }

    private double interpolate(List<DifficultyBracket> sorted, double pi, boolean hp) {
        double prevPi = 0;
        double prevVal = 1.0;
        for (DifficultyBracket b : sorted) {
            double endPi = b.maxPi();
            double endVal = hp ? b.hpScale() : b.dmgScale();
            if (pi <= endPi || b == sorted.get(sorted.size() - 1)) {
                if (endPi <= prevPi) {
                    return endVal;
                }
                double t = Math.max(0, Math.min(1, (pi - prevPi) / (endPi - prevPi)));
                return prevVal + t * (endVal - prevVal);
            }
            prevPi = endPi;
            prevVal = endVal;
        }
        return prevVal;
    }

    /** Solo (non-multiplayer-pooled) sell bonus for a single player's own bracket, up to 1.5x at T5. */
    public double sellBonusMultiplier(java.util.UUID uuid) {
        double pi = economyAPI.getProgressionIndex(uuid);
        return bracketFor(pi).sellBonus();
    }
}
