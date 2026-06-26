package com.tide.core.death;

import com.tide.core.economy.EconomyAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * "조류에 휩쓸림" soft-hardcore death. Runs at NORMAL priority so TideRPG's
 * deep-mine death handler (LOWEST) always runs first and can opt a player out
 * via the "tide_deepmine_death" metadata flag — deep mine deaths use their
 * own forfeit-some-loot rule instead of a grave.
 */
public final class DeathListener implements Listener {

    private static final String DEEPMINE_DEATH_METADATA = "tide_deepmine_death";

    private final JavaPlugin plugin;
    private final EconomyAPI economyAPI;
    private final GraveManager graveManager;
    private final Set<UUID> pendingRespawnDebuff = ConcurrentHashMap.newKeySet();

    public DeathListener(JavaPlugin plugin, EconomyAPI economyAPI, GraveManager graveManager) {
        this.plugin = plugin;
        this.economyAPI = economyAPI;
        this.graveManager = graveManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasMetadata(DEEPMINE_DEATH_METADATA)) {
            player.removeMetadata(DEEPMINE_DEATH_METADATA, plugin);
            return;
        }

        int lossPercent = plugin.getConfig().getInt("death_penalty.clam-loss-percent", 10);
        long currentClam = economyAPI.getClam(player.getUniqueId());
        long loss = currentClam * lossPercent / 100;
        if (loss > 0) {
            economyAPI.takeClam(player.getUniqueId(), loss);
        }

        if (economyAPI.isHardMode(player.getUniqueId())) {
            applyHardModeReinforceDowngrade(player);
        }

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();

        long duration = plugin.getConfig().getLong("death_penalty.grave-duration-seconds", 600);
        graveManager.create(player, player.getLocation(), drops, duration);

        pendingRespawnDebuff.add(player.getUniqueId());
        player.sendMessage("§c조류에 휩쓸렸습니다! §7조개 " + loss + "개를 잃었고, 사망 지점에 유실물 비석이 생성되었습니다.");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingRespawnDebuff.remove(player.getUniqueId())) {
            return;
        }
        int debuffSeconds = plugin.getConfig().getInt("death_penalty.debuff-duration-seconds", 60);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, debuffSeconds * 20, 1));
    }

    private void applyHardModeReinforceDowngrade(Player player) {
        ItemStack best = null;
        int bestReinforce = -1;
        for (ItemStack equipped : player.getInventory().getArmorContents()) {
            int level = reinforceOf(equipped);
            if (level > bestReinforce) {
                bestReinforce = level;
                best = equipped;
            }
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (reinforceOf(mainHand) > bestReinforce) {
            best = mainHand;
            bestReinforce = reinforceOf(mainHand);
        }
        if (best == null || bestReinforce <= 0) {
            return;
        }
        ItemMeta meta = best.getItemMeta();
        meta.getPersistentDataContainer().set(GearPdcKeys.REINFORCE, PersistentDataType.INTEGER, bestReinforce - 1);
        best.setItemMeta(meta);
        player.sendMessage("§4[하드 모드] §c최고 강화 장비의 강화 단계가 1 하락했습니다.");
    }

    private int reinforceOf(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return -1;
        }
        Integer level = itemStack.getItemMeta().getPersistentDataContainer()
                .get(GearPdcKeys.REINFORCE, PersistentDataType.INTEGER);
        return level == null ? -1 : level;
    }
}
