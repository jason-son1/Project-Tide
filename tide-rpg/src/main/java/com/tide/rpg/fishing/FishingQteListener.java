package com.tide.rpg.fishing;

import com.tide.core.economy.EconomyAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Layers a reaction-time QTE on top of vanilla fishing inside registered
 * Mythic Fishing Hole zones. Vanilla already gates BITE -> CAUGHT_FISH behind
 * the player's own right-click timing; we just grade how fast that click was.
 */
public final class FishingQteListener implements Listener {

    private static final long PERFECT_WINDOW_MS = 600;
    private static final long GOOD_WINDOW_MS = 1500;

    private final FishingHoleRegistry fishingHoleRegistry;
    private final EconomyAPI economyAPI;
    private final Map<UUID, Long> biteTimestamps = new ConcurrentHashMap<>();

    public FishingQteListener(FishingHoleRegistry fishingHoleRegistry, EconomyAPI economyAPI) {
        this.fishingHoleRegistry = fishingHoleRegistry;
        this.economyAPI = economyAPI;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!fishingHoleRegistry.isInsideAny(player.getLocation())) {
            return;
        }

        if (event.getState() == PlayerFishEvent.State.BITE) {
            biteTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§b🎣 지금 우클릭하세요!"));
            return;
        }

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Long biteAt = biteTimestamps.remove(player.getUniqueId());
            if (biteAt == null) {
                return;
            }
            long reactionMs = System.currentTimeMillis() - biteAt;
            grade(player, reactionMs);
        } else {
            biteTimestamps.remove(player.getUniqueId());
        }
    }

    private void grade(Player player, long reactionMs) {
        com.tide.core.effect.EffectEngine effectEngine = org.bukkit.Bukkit.getServicesManager().load(com.tide.core.effect.EffectEngine.class);
        if (reactionMs <= PERFECT_WINDOW_MS) {
            int pearls = ThreadLocalRandom.current().nextInt(1, 4);
            economyAPI.addPearl(player.getUniqueId(), pearls);
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 20 * 60 * 5, 0));
            player.sendMessage("§a§l[완벽한 손맛!] §f진주 " + pearls + "개 §7+ §d행운 물약 효과 5분");
            if (effectEngine != null) {
                effectEngine.playEffect(player, "fishing_qte_perfect");
            }
        } else if (reactionMs <= GOOD_WINDOW_MS) {
            long clam = ThreadLocalRandom.current().nextInt(20, 51);
            economyAPI.addClam(player.getUniqueId(), clam);
            player.sendMessage("§e[좋은 손맛] §6조개 " + clam + "개 획득");
            if (effectEngine != null) {
                effectEngine.playEffect(player, "fishing_qte_good");
            }
        }
    }
}
