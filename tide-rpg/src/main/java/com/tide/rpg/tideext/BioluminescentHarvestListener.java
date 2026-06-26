package com.tide.rpg.tideext;

import com.tide.rpg.TideKeys;
import com.tide.rpg.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

public final class BioluminescentHarvestListener implements Listener {

    private static final double AMBUSH_CHANCE = 0.20;

    private final BioluminescentManager bioluminescentManager;
    private final ItemFactory itemFactory;

    public BioluminescentHarvestListener(BioluminescentManager bioluminescentManager, ItemFactory itemFactory) {
        this.bioluminescentManager = bioluminescentManager;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getClickedBlock().getType() != Material.GLOW_LICHEN) {
            return;
        }
        if (!bioluminescentManager.isActiveSpore(event.getClickedBlock().getLocation())) {
            return;
        }
        if (!isExtractor(event.getItem())) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        bioluminescentManager.removeSpore(event.getClickedBlock().getLocation());
        event.getClickedBlock().setType(Material.AIR);

        int amount = ThreadLocalRandom.current().nextInt(1, 4);
        for (int i = 0; i < amount; i++) {
            player.getInventory().addItem(itemFactory.create("bioluminescent_essence"));
        }
        player.getWorld().spawnParticle(Particle.GLOW, event.getClickedBlock().getLocation(), 20, 0.4, 0.4, 0.4);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        player.sendMessage("§a생물발광 에센스 §f" + amount + "개§a를 채집했습니다.");

        if (ThreadLocalRandom.current().nextDouble() < AMBUSH_CHANCE) {
            var drowned = player.getWorld().spawnEntity(
                    event.getClickedBlock().getLocation().add(ThreadLocalRandom.current().nextInt(-3, 4), 0,
                            ThreadLocalRandom.current().nextInt(-3, 4)),
                    EntityType.DROWNED);
            drowned.getPersistentDataContainer().set(TideKeys.ELITE, PersistentDataType.BYTE, (byte) 0);
            player.sendMessage("§c빛을 쫓는 무언가가 다가옵니다...");
        }
    }

    private boolean isExtractor(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(TideKeys.ITEM_ID, PersistentDataType.STRING);
        return "tide_extractor".equals(id);
    }
}
