package com.tide.rpg.deepmine;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DeepMineCommand implements CommandExecutor {

    private final DeepMineManager manager;

    public DeepMineCommand(DeepMineManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("enter")) {
            manager.enter(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("leave")) {
            manager.leave(player);
            return true;
        }
        player.sendMessage("§c사용법: /deepmine [enter|leave]");
        return true;
    }
}
