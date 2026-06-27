package com.tide.rpg.item;

import com.tide.rpg.TideKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * ItemFactory.create("iron_sword_t1"): loads the yml definition, stamps every
 * RPG value into the PDC, then asks LoreRenderer to render the display-only Lore.
 */
public final class ItemFactory {

    private final ItemRegistry itemRegistry;
    private final LoreRenderer loreRenderer;

    public ItemFactory(ItemRegistry itemRegistry, LoreRenderer loreRenderer) {
        this.itemRegistry = itemRegistry;
        this.loreRenderer = loreRenderer;
    }

    public ItemStack create(String itemId) {
        ItemDefinition definition = itemRegistry.get(itemId);
        if (definition == null) {
            throw new IllegalArgumentException("알 수 없는 아이템 ID: " + itemId);
        }

        ItemStack itemStack = new ItemStack(definition.getMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(definition.getDisplayName());
        if (definition.getCustomModelData() > 0) {
            meta.setCustomModelData(definition.getCustomModelData());
        }

        meta.getPersistentDataContainer().set(TideKeys.ITEM_ID, PersistentDataType.STRING, definition.getId());
        meta.getPersistentDataContainer().set(TideKeys.GS, PersistentDataType.INTEGER, definition.getGearScore());
        meta.getPersistentDataContainer().set(TideKeys.REINFORCE, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(TideKeys.SOCKET_COUNT, PersistentDataType.INTEGER, definition.getSocketCount());
        meta.getPersistentDataContainer().set(TideKeys.CMD, PersistentDataType.INTEGER, definition.getCustomModelData());
        if (definition.getResonance() != null && !definition.getResonance().isBlank()) {
            meta.getPersistentDataContainer().set(TideKeys.RESONANCE, PersistentDataType.STRING, definition.getResonance());
        }
        if (definition.getAnchor() > 0) {
            meta.getPersistentDataContainer().set(TideKeys.ANCHOR, PersistentDataType.INTEGER, definition.getAnchor());
        }
        if (definition.getOxygenCapacity() > 0) {
            meta.getPersistentDataContainer().set(TideKeys.OXYGEN_CAPACITY, PersistentDataType.INTEGER, definition.getOxygenCapacity());
        }
        meta.setEnchantmentGlintOverride(true);
        itemStack.setItemMeta(meta);

        ItemStatApplier.apply(itemStack, definition);
        loreRenderer.render(itemStack);
        return itemStack;
    }

    public java.util.Collection<String> getRegisteredIds() {
        return itemRegistry.getAll().keySet();
    }
}
