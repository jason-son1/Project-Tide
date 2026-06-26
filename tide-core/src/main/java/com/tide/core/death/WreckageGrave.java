package com.tide.core.death;

import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class WreckageGrave {

    private final UUID graveId;
    private final UUID ownerUuid;
    private final ArmorStand armorStand;
    private final List<ItemStack> items;
    private long remainingSeconds;

    public WreckageGrave(UUID graveId, UUID ownerUuid, ArmorStand armorStand, List<ItemStack> items, long remainingSeconds) {
        this.graveId = graveId;
        this.ownerUuid = ownerUuid;
        this.armorStand = armorStand;
        this.items = items;
        this.remainingSeconds = remainingSeconds;
    }

    public UUID getGraveId() {
        return graveId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public ArmorStand getArmorStand() {
        return armorStand;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void decrementSecond() {
        remainingSeconds = Math.max(0, remainingSeconds - 1);
    }
}
