package com.tide.core.commands;

import com.tide.core.admin.AdminGUI;
import com.tide.core.reload.ReloadManager;
import com.tide.core.tide.TideScheduler;
import com.tide.core.tide.TideState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class TideCommand implements CommandExecutor {

    private final TideScheduler scheduler;
    private final ReloadManager reloadManager;
    private final AdminGUI adminGUI;

    public TideCommand(TideScheduler scheduler, ReloadManager reloadManager, AdminGUI adminGUI) {
        this.scheduler = scheduler;
        this.reloadManager = reloadManager;
        this.adminGUI = adminGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§b현재 조수: " + scheduler.getCurrentState().getDisplayName()
                    + " §7(남은 시간: " + scheduler.getSecondsUntilNextChange() + "초)");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender, args);
            case "set" -> handleSet(sender, args);
            case "admin" -> handleAdmin(sender);
            case "tempo" -> handleTempo(sender, args);
            default -> sender.sendMessage("§c사용법: /tide [reload [대상]|set <상태>|admin|tempo <분>]");
        }
        return true;
    }

    private void handleAdmin(CommandSender sender) {
        if (!sender.hasPermission("tide.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return;
        }
        adminGUI.open(player);
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tide.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return;
        }
        if (args.length < 2) {
            Map<String, Integer> results = reloadManager.reloadAll();
            results.forEach((name, count) -> sender.sendMessage("§a[리로드] " + name + " §7- " + count + "건"));
            if (results.isEmpty()) {
                sender.sendMessage("§7리로드 가능한 대상이 없습니다.");
            }
            return;
        }
        String target = args[1];
        Integer count = reloadManager.reload(target);
        if (count == null) {
            sender.sendMessage("§c알 수 없는 리로드 대상: " + target);
        } else {
            sender.sendMessage("§a[리로드] " + target + " §7- " + count + "건 완료");
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tide.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c사용법: /tide set <HIGH_TIDE|LOW_TIDE|SPRING_TIDE|BLOOD_MOON|BLOOD_TIDE>");
            return;
        }
        try {
            TideState state = TideState.valueOf(args[1].toUpperCase());
            scheduler.forceState(state);
            sender.sendMessage("§a조수 상태를 " + state.getDisplayName() + " §a로 변경했습니다.");
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c알 수 없는 상태: " + args[1]);
        }
    }

    private void handleTempo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tide.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c사용법: /tide tempo <분 단위 시간>");
            return;
        }
        try {
            long minutes = Long.parseLong(args[1]);
            if (minutes < 1) {
                sender.sendMessage("§c시간은 최소 1분 이상이어야 합니다.");
                return;
            }
            scheduler.setCycleDurationMinutes(minutes);
            sender.sendMessage("§a조수 주기 템포를 §f" + minutes + "분§a으로 조정했습니다.");
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c올바른 숫자를 입력해주세요.");
        }
    }
}
