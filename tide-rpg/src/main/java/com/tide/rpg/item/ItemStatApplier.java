package com.tide.rpg.item;

import com.tide.rpg.TideKeys;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Stamps base_stats/reinforce_bonus (items/*.yml) onto the item as real
 * vanilla AttributeModifiers. Without this, "강화"(reinforce) only changed the
 * displayed GS/Lore numbers — the gear dealt/blocked exactly the same damage
 * regardless of reinforce level. Must be re-applied any time the PDC
 * REINFORCE value changes (item creation, forge reinforce success/failure).
 */
public final class ItemStatApplier {

    private static final NamespacedKey DAMAGE_KEY = new NamespacedKey(TideKeys.NAMESPACE, "gear_damage");
    private static final NamespacedKey ARMOR_KEY = new NamespacedKey(TideKeys.NAMESPACE, "gear_armor");

    private ItemStatApplier() {
    }

    public static void apply(ItemStack itemStack, ItemDefinition definition) {
        if (definition == null) {
            return;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        int reinforce = meta.getPersistentDataContainer()
                .getOrDefault(TideKeys.REINFORCE, PersistentDataType.INTEGER, 0);

        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);

        if (definition.getBaseDamage() > 0 || definition.getDamagePerStar() > 0) {
            double total = definition.getBaseDamage() + reinforce * definition.getDamagePerStar();
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                    DAMAGE_KEY, total, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
        }
        if (definition.getBaseDefense() > 0 || definition.getDefensePerStar() > 0) {
            double total = definition.getBaseDefense() + reinforce * definition.getDefensePerStar();
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                    ARMOR_KEY, total, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ARMOR));
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        itemStack.setItemMeta(meta);
    }
}
