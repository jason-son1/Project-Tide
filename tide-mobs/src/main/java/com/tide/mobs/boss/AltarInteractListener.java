package com.tide.mobs.boss;

import com.tide.rpg.TideKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class AltarInteractListener implements Listener {

    private final AltarRegistry altarRegistry;
    private final BossFightManager bossFightManager;

    public AltarInteractListener(AltarRegistry altarRegistry, BossFightManager bossFightManager) {
        this.altarRegistry = altarRegistry;
        this.bossFightManager = bossFightManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        SoulAltar altar = altarRegistry.findAt(event.getClickedBlock().getLocation());
        if (altar == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (bossFightManager.hasActiveFight(altar.getId())) {
            player.sendMessage("§c이미 이 제단에서 전투가 진행 중입니다.");
            return;
        }

        int owned = countSoulFragments(player);
        if (owned < altar.getRequiredFragments()) {
            player.sendMessage("§c영혼 파편이 부족합니다. (" + owned + "/" + altar.getRequiredFragments() + ")");
            return;
        }

        consumeSoulFragments(player, altar.getRequiredFragments());
        bossFightManager.summon(player, altar);
    }

    private int countSoulFragments(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSoulFragment(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void consumeSoulFragments(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (!isSoulFragment(item)) {
                continue;
            }
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            remaining -= take;
        }
    }

    private boolean isSoulFragment(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(TideKeys.ITEM_ID, PersistentDataType.STRING);
        return "soul_fragment".equals(id);
    }
}
