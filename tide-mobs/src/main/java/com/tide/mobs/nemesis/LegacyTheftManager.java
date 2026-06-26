package com.tide.mobs.nemesis;

import com.tide.mobs.MobKeys;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Legacy Theft (3-2): instead of a player's best-reinforced gear quietly
 * sitting in their wreckage grave, the nemesis that killed them wears it —
 * visibly and with its actual stat bonus — until someone kills that nemesis,
 * at which point the original owner gets it back.
 */
public final class LegacyTheftManager {

    private final Map<UUID, StolenItem> stolenByMob = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> pendingDelivery = new ConcurrentHashMap<>();

    private record StolenItem(UUID ownerUuid, ItemStack item) {
    }

    /** Picks the single highest-reinforce item out of a death's drop list and has the mob wear it instead. */
    public void stealBestItem(LivingEntity mob, Player owner, List<ItemStack> drops) {
        ItemStack best = null;
        int bestReinforce = -1;
        for (ItemStack candidate : drops) {
            int level = reinforceOf(candidate);
            if (level > bestReinforce) {
                bestReinforce = level;
                best = candidate;
            }
        }
        if (best == null || bestReinforce <= 0) {
            return;
        }
        drops.remove(best);

        ItemMeta meta = best.getItemMeta();
        meta.getPersistentDataContainer().set(MobKeys.STOLEN_FROM, PersistentDataType.STRING, owner.getUniqueId().toString());
        best.setItemMeta(meta);

        equip(mob, best);
        stolenByMob.put(mob.getUniqueId(), new StolenItem(owner.getUniqueId(), best));
        owner.sendMessage("§4네메시스가 당신의 §f" + displayName(best) + " §4를 빼앗아 착용했습니다!");
    }

    private void equip(LivingEntity mob, ItemStack item) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return;
        }
        String typeName = item.getType().name();
        if (typeName.contains("HELMET") || typeName.contains("SKULL") || typeName.contains("HEAD")) {
            equipment.setHelmet(item);
        } else if (typeName.contains("CHESTPLATE")) {
            equipment.setChestplate(item);
        } else if (typeName.contains("LEGGINGS")) {
            equipment.setLeggings(item);
        } else if (typeName.contains("BOOTS")) {
            equipment.setBoots(item);
        } else {
            equipment.setItemInMainHand(item);
        }
    }

    /** @return the recovered item, or null if this mob hadn't stolen anything. */
    public ItemStack recover(UUID mobUuid) {
        StolenItem stolen = stolenByMob.remove(mobUuid);
        if (stolen == null) {
            return null;
        }
        Player owner = Bukkit.getPlayer(stolen.ownerUuid());
        if (owner != null && owner.isOnline()) {
            owner.getInventory().addItem(stolen.item());
            owner.sendMessage("§a네메시스를 처치하여 빼앗긴 §f" + displayName(stolen.item()) + " §a를 되찾았습니다!");
        } else {
            pendingDelivery.computeIfAbsent(stolen.ownerUuid(), uuid -> new ArrayList<>()).add(stolen.item());
        }
        return stolen.item();
    }

    public void deliverPending(Player player) {
        List<ItemStack> items = pendingDelivery.remove(player.getUniqueId());
        if (items == null) {
            return;
        }
        for (ItemStack item : items) {
            player.getInventory().addItem(item);
        }
        player.sendMessage("§a네메시스에게 빼앗겼던 장비 " + items.size() + "개를 되찾았습니다.");
    }

    private int reinforceOf(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return -1;
        }
        Integer level = itemStack.getItemMeta().getPersistentDataContainer()
                .get(com.tide.rpg.TideKeys.REINFORCE, PersistentDataType.INTEGER);
        return level == null ? -1 : level;
    }

    private String displayName(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : itemStack.getType().name();
    }
}
