package com.tide.rpg.item;

import com.tide.core.TideCorePlugin;
import com.tide.core.tide.TideState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

public final class TideVaultListener implements Listener {

    private static final NamespacedKey VAULT_KEY = new NamespacedKey("tide", "vault");
    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("tide", "item_id");

    private final ItemFactory itemFactory;

    public TideVaultListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }

        boolean isVault = false;

        // Check block PDC
        if (block.getState() instanceof TileState tileState) {
            String value = tileState.getPersistentDataContainer().get(VAULT_KEY, PersistentDataType.STRING);
            if ("tide_vault".equalsIgnoreCase(value)) {
                isVault = true;
            }
        }

        // Check chest custom name fallback
        if (!isVault && block.getState() instanceof org.bukkit.block.Chest chest) {
            String customName = chest.getCustomName();
            if (customName != null && (customName.contains("조수 금고") || customName.contains("Tide Vault"))) {
                isVault = true;
            }
        }

        if (!isVault) {
            return;
        }

        // Vault detected! Cancel normal chest opening
        event.setCancelled(true);
        Player player = event.getPlayer();

        // Check Tide State
        TideState currentState = TideCorePlugin.getInstance().getTideScheduler().getCurrentState();
        boolean isTimeAllowed = currentState == TideState.SPRING_TIDE || 
                              currentState == TideState.BLOOD_MOON || 
                              currentState == TideState.BLOOD_TIDE;

        if (!isTimeAllowed) {
            player.sendMessage("§c[조수 금고] 이 금고는 사리(Spring Tide) 또는 블러드문(Blood Moon) 시기에만 개방됩니다!");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            return;
        }

        // Check Key Items
        ItemStack keyItem = null;
        int keySlot = -1;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            String itemId = meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
            if (itemId != null && (itemId.equals("soul_fragment") || itemId.equals("nemesis_token"))) {
                keyItem = item;
                keySlot = i;
                break;
            }
        }

        if (keyItem == null) {
            player.sendMessage("§c[조수 금고] 열쇠가 필요합니다! (영혼 파편 또는 네메시스의 징표를 지참하세요.)");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            return;
        }

        // Consume 1 key
        keyItem.setAmount(keyItem.getAmount() - 1);
        if (keyItem.getAmount() <= 0) {
            player.getInventory().setItem(keySlot, null);
        } else {
            player.getInventory().setItem(keySlot, keyItem);
        }

        // Open Reward GUI
        openRewardInventory(player);
    }

    private void openRewardInventory(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        Inventory rewardInv = Bukkit.createInventory(null, 27, "§6§l조수 금고 보상 창");

        // Spawn 3-5 premium items
        int rewardCount = ThreadLocalRandom.current().nextInt(3, 6);
        for (int i = 0; i < rewardCount; i++) {
            int slot = ThreadLocalRandom.current().nextInt(27);
            while (rewardInv.getItem(slot) != null) {
                slot = ThreadLocalRandom.current().nextInt(27);
            }

            // High tier reward selection
            ItemStack reward = null;
            if (itemFactory != null) {
                double roll = ThreadLocalRandom.current().nextDouble();
                if (roll < 0.15) {
                    reward = itemFactory.create("flame_sword_t1");
                } else if (roll < 0.50) {
                    reward = new ItemStack(Material.DIAMOND, ThreadLocalRandom.current().nextInt(2, 6));
                } else {
                    reward = new ItemStack(Material.IRON_INGOT, ThreadLocalRandom.current().nextInt(5, 13));
                }
            } else {
                reward = new ItemStack(Material.GOLD_INGOT, ThreadLocalRandom.current().nextInt(3, 8));
            }

            rewardInv.setItem(slot, reward);
        }

        player.openInventory(rewardInv);
        player.sendMessage("§a[조수 금고] 보물 창고가 개방되었습니다!");
    }
}
