package com.tide.mobs.affix;

import com.tide.core.difficulty.DifficultyManager;
import com.tide.core.economy.EconomyAPI;
import com.tide.rpg.TideKeys;
import com.tide.rpg.item.ItemFactory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Elite-only drop table addition, per EliteProcessor step 7 in the spec.
 * Also grants a small WDM-scaled clam bonus on every elite kill (Dynamic
 * World Difficulty Scaling spec 5장) — previously elites granted no
 * currency at all here, only the flat soul_fragment chance.
 */
public final class EliteDropListener implements Listener {

    private static final double SOUL_FRAGMENT_CHANCE = 0.08;
    private static final long BASE_CLAM_REWARD = 10;

    private final ItemFactory itemFactory;
    private final EconomyAPI economyAPI;
    private final DifficultyManager difficultyManager;

    public EliteDropListener(ItemFactory itemFactory, EconomyAPI economyAPI, DifficultyManager difficultyManager) {
        this.itemFactory = itemFactory;
        this.economyAPI = economyAPI;
        this.difficultyManager = difficultyManager;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        boolean isElite = entity.getPersistentDataContainer()
                .getOrDefault(TideKeys.ELITE, PersistentDataType.BYTE, (byte) 0) == 1;
        if (!isElite) {
            return;
        }
        if (itemFactory != null && ThreadLocalRandom.current().nextDouble() < SOUL_FRAGMENT_CHANCE) {
            event.getDrops().add(itemFactory.create("soul_fragment"));
        }

        Player killer = entity.getKiller();
        if (killer == null || economyAPI == null) {
            return;
        }
        double dropBonus = 1.0;
        if (difficultyManager != null && difficultyManager.isEnabled()) {
            dropBonus = 1.0 + 0.5 * (difficultyManager.resolve(entity.getLocation()).hpMultiplier() - 1.0);
        }
        long clamReward = Math.round(BASE_CLAM_REWARD * dropBonus);
        if (clamReward > 0) {
            economyAPI.addClam(killer.getUniqueId(), clamReward);
        }
    }
}
