package com.tide.core.hud;

import com.tide.core.death.GraveManager;
import com.tide.core.economy.EconomyAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sidebar HUD. Shows the grave compass (5-3) while a WreckageGrave is active,
 * otherwise the normal clam/pearl/rep readout. Refreshed once per second —
 * not a per-tick scan, and only over the (small) online player list.
 */
public final class EconomyScoreboardHud {

    private static final String OBJECTIVE_NAME = "tide_hud";

    private final JavaPlugin plugin;
    private final EconomyAPI economyAPI;
    private final GraveManager graveManager;
    private final Map<UUID, List<String>> lastLines = new ConcurrentHashMap<>();
    private BukkitTask task;

    public EconomyScoreboardHud(JavaPlugin plugin, EconomyAPI economyAPI, GraveManager graveManager) {
        this.plugin = plugin;
        this.economyAPI = economyAPI;
        this.graveManager = graveManager;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public void setup(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy", "§6§l🌊 The Tide");
        scoreboard.getObjective(OBJECTIVE_NAME).setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private void refresh(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            setup(player);
            return;
        }

        List<String> newLines = buildLines(player);
        List<String> previous = lastLines.getOrDefault(player.getUniqueId(), List.of());
        for (String old : previous) {
            scoreboard.resetScores(old);
        }

        int score = newLines.size();
        for (String line : newLines) {
            Score scoreEntry = objective.getScore(line);
            scoreEntry.setScore(score--);
        }
        lastLines.put(player.getUniqueId(), newLines);
    }

    private List<String> buildLines(Player player) {
        List<String> lines = new ArrayList<>();
        if (graveManager.hasActiveGrave(player.getUniqueId())) {
            lines.addAll(graveManager.buildHudLines(player));
            lines.add("§7──────────────");
        }
        UUID uuid = player.getUniqueId();
        lines.add("§6조개: §f" + economyAPI.getClam(uuid));
        lines.add("§d진주: §f" + economyAPI.getPearl(uuid));
        lines.add("§a평판: §f" + economyAPI.getRepTier(uuid));
        return lines;
    }
}
