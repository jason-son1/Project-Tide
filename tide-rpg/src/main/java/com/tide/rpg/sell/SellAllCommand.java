package com.tide.rpg.sell;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SellAllCommand implements CommandExecutor {

    private final SellAllManager sellAllManager;

    public SellAllCommand(SellAllManager sellAllManager) {
        this.sellAllManager = sellAllManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            SellAllManager.Preview result = sellAllManager.confirm(player);
            if (result.totalClam() <= 0) {
                player.sendMessage("§7판매할 잡템이 없습니다.");
                return true;
            }
            player.sendMessage("§a판매 완료! §6총 " + result.totalClam() + " 조개를 획득했습니다.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            player.sendMessage("§7판매를 취소했습니다.");
            return true;
        }

        SellAllManager.Preview preview = sellAllManager.preview(player);
        if (preview.totalClam() <= 0) {
            player.sendMessage("§7판매할 잡템이 없습니다. (장비는 자동 제외됩니다)");
            return true;
        }
        preview.countsByLabel().forEach((label2, amount) ->
                player.sendMessage("§7[판매 예정] " + label2 + " x" + amount));
        player.sendMessage("§6총 " + preview.totalClam() + " 조개 §7- §a[확인] /sellall confirm §7또는 §c[취소] /sellall cancel");
        return true;
    }
}
