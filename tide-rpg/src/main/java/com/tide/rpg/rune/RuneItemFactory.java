package com.tide.rpg.rune;

import com.tide.rpg.TideKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Mirrors ItemFactory but for rune items — the physical ItemStack a player drags into a socket. */
public final class RuneItemFactory {

    private final RuneRegistry runeRegistry;

    public RuneItemFactory(RuneRegistry runeRegistry) {
        this.runeRegistry = runeRegistry;
    }

    public ItemStack create(String runeId) {
        RuneDefinition definition = runeRegistry.getAll().get(runeId);
        if (definition == null) {
            throw new IllegalArgumentException("알 수 없는 룬 ID: " + runeId);
        }
        ItemStack itemStack = new ItemStack(definition.getMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(definition.getDisplayName());
        if (definition.getCustomModelData() > 0) {
            meta.setCustomModelData(definition.getCustomModelData());
        }
        meta.getPersistentDataContainer().set(TideKeys.RUNE_ID, PersistentDataType.STRING, definition.getId());
        meta.setLore(java.util.List.of("§7타입: §f" + definition.getType(),
                "§7등급: §f" + definition.getGrade()));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /** @return the rune id stored on this item's PDC, or null if it isn't a Tide rune item. */
    public String readRuneId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return null;
        }
        return itemStack.getItemMeta().getPersistentDataContainer()
                .get(TideKeys.RUNE_ID, PersistentDataType.STRING);
    }
}
