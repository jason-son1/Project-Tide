package com.tide.rpg.gs;

import com.tide.rpg.TideKeys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * playerGS = sum of equipped armor + main hand weapon PDC "gs", each boosted
 * by its own reinforce level (gs + reinforce * gsPerStar), per the 5-3 spec.
 */
public final class GearScoreCalculator {

    private final double gsPerReinforceStar;

    public GearScoreCalculator(double gsPerReinforceStar) {
        this.gsPerReinforceStar = gsPerReinforceStar;
    }

    public int calculate(Player player) {
        PlayerInventory inventory = player.getInventory();
        int total = 0;
        total += gsOf(inventory.getHelmet());
        total += gsOf(inventory.getChestplate());
        total += gsOf(inventory.getLeggings());
        total += gsOf(inventory.getBoots());
        total += gsOf(inventory.getItemInMainHand());
        return total;
    }

    private int gsOf(ItemStack itemStack) {
        if (itemStack == null) {
            return 0;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return 0;
        }
        var pdc = meta.getPersistentDataContainer();
        Integer gs = pdc.get(TideKeys.GS, PersistentDataType.INTEGER);
        if (gs == null) {
            return 0;
        }
        int reinforce = pdc.getOrDefault(TideKeys.REINFORCE, PersistentDataType.INTEGER, 0);
        return (int) Math.round(gs + reinforce * gsPerReinforceStar);
    }
}
