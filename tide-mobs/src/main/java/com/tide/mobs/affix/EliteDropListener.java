package com.tide.mobs.affix;

import com.tide.rpg.TideKeys;
import com.tide.rpg.item.ItemFactory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/** Elite-only drop table addition, per EliteProcessor step 7 in the spec. */
public final class EliteDropListener implements Listener {

    private static final double SOUL_FRAGMENT_CHANCE = 0.08;

    private final ItemFactory itemFactory;

    public EliteDropListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        boolean isElite = entity.getPersistentDataContainer()
                .getOrDefault(TideKeys.ELITE, PersistentDataType.BYTE, (byte) 0) == 1;
        if (!isElite || itemFactory == null) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < SOUL_FRAGMENT_CHANCE) {
            event.getDrops().add(itemFactory.create("soul_fragment"));
        }
    }
}
