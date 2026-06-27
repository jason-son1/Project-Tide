package com.tide.mobs.quest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class BountyBoardListener implements Listener {

    private final BountyManager bountyManager;
    private final BountyBoardGUI bountyBoardGUI;

    public BountyBoardListener(BountyManager bountyManager, BountyBoardGUI bountyBoardGUI) {
        this.bountyManager = bountyManager;
        this.bountyBoardGUI = bountyBoardGUI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BountyBoardGUI.Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int index = event.getRawSlot();
        if (index == 8) {
            com.tide.core.TideCorePlugin corePlugin = com.tide.core.TideCorePlugin.getPlugin(com.tide.core.TideCorePlugin.class);
            new com.tide.core.guide.GuideGUI(corePlugin.getGuideRegistry()).openEntries(player, com.tide.core.guide.GuideCategory.BOUNTY);
            return;
        }

        var quests = holder.getQuests();
        int questIndex = -1;
        for (int i = 0; i < quests.size(); i++) {
            int slot = i * 2 + 1 <= 26 ? i * 2 + 1 : i;
            if (slot == index) {
                questIndex = i;
                break;
            }
        }
        if (questIndex < 0) {
            return;
        }
        BountyQuest quest = quests.get(questIndex);
        if (bountyManager.claim(player, quest)) {
            player.sendMessage("§a보상을 수령했습니다: §6조개 " + quest.getRewardClam() + " §a평판 " + quest.getRewardRep());
            event.getInventory().setItem(index, bountyBoardGUI.renderQuest(quest));
        }
    }
}
