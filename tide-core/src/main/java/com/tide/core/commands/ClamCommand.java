package com.tide.core.commands;

import com.tide.core.economy.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Debug command from the foundation stage — confirms profiles load/save correctly. */
public final class ClamCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public ClamCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        long clam = economyManager.getClam(player.getUniqueId());
        player.sendMessage("§6조개(Clam): §f" + clam);
        return true;
    }
}
