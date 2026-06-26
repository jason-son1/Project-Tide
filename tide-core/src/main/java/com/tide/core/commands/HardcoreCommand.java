package com.tide.core.commands;

import com.tide.core.economy.EconomyAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HardcoreCommand implements CommandExecutor {

    private final EconomyAPI economyAPI;

    public HardcoreCommand(EconomyAPI economyAPI) {
        this.economyAPI = economyAPI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        boolean newState = !economyAPI.isHardMode(player.getUniqueId());
        economyAPI.setHardMode(player.getUniqueId(), newState);
        player.sendMessage(newState
                ? "§4하드 모드가 활성화되었습니다. §7사망 시 최고 강화 장비의 강화 단계가 1 하락합니다."
                : "§a하드 모드가 비활성화되었습니다.");
        return true;
    }
}
