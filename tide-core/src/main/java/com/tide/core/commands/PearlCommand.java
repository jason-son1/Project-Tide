package com.tide.core.commands;

import com.tide.core.economy.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Debug command from the foundation stage — confirms profiles load/save correctly. */
public final class PearlCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public PearlCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        long pearl = economyManager.getPearl(player.getUniqueId());
        player.sendMessage("§d진주(Pearl): §f" + pearl);
        return true;
    }
}
