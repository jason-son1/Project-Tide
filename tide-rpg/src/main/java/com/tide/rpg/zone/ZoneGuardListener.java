package com.tide.rpg.zone;

import com.tide.rpg.gs.GearScoreCalculator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Only re-evaluates when the player crosses a chunk boundary (per the "PlayerMoveEvent
 * (청크 경계 체크로 최적화)" note) — never runs the zone/GS lookup on every single move tick.
 */
public final class ZoneGuardListener implements Listener {

    private static final long WARNING_COOLDOWN_MS = 8000;

    private final ZoneRegistry zoneRegistry;
    private final GearScoreCalculator gearScoreCalculator;
    private final Map<UUID, Long> lastWarningAt = new ConcurrentHashMap<>();

    public ZoneGuardListener(ZoneRegistry zoneRegistry, GearScoreCalculator gearScoreCalculator) {
        this.zoneRegistry = zoneRegistry;
        this.gearScoreCalculator = gearScoreCalculator;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }

        Player player = event.getPlayer();
        ZoneDefinition zone = zoneRegistry.findZoneAt(event.getTo());
        if (zone == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastWarningAt.get(player.getUniqueId());
        if (last != null && now - last < WARNING_COOLDOWN_MS) {
            return;
        }

        int playerGs = gearScoreCalculator.calculate(player);
        if (playerGs < zone.getWarnGs()) {
            player.sendTitle("§c⚠ 위험", "§f현재 GS: " + playerGs + " §7/ 권장: " + zone.getRecommendedGs(), 10, 60, 10);
            lastWarningAt.put(player.getUniqueId(), now);
        } else if (playerGs < zone.getRecommendedGs()) {
            player.sendTitle("§e주의", "§f현재 GS: " + playerGs + " §7/ 권장: " + zone.getRecommendedGs(), 10, 60, 10);
            lastWarningAt.put(player.getUniqueId(), now);
        }
    }
}
