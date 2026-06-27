package com.tide.rpg.deepmine;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /deepmine [enter|leave] [id] — normal player-facing teleport (admin-gated
 * for now; regular players are meant to discover the portal organically).
 * /deepmine info [id] — admin diagnostic: prints world/bounds/entrance
 * coordinates without teleporting, for every instance if no id is given.
 * /deepmine create — automatically allocates a new non-overlapping dungeon
 * instance next to the existing ones and starts it immediately, instead of
 * requiring an admin to hand-pick coordinates in config.yml.
 */
public final class DeepMineCommand implements CommandExecutor, TabCompleter {

    private final DeepMineManagerRegistry registry;

    public DeepMineCommand(DeepMineManagerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (!player.hasPermission("tide.admin")) {
            player.sendMessage("§c일반 플레이어는 명령어로 이동할 수 없습니다. 광질 도중 우연히 발견되는 광산 통로를 직접 찾으세요!");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("enter")) {
            DeepMineManager manager = resolveManager(player, args, 1);
            if (manager != null) {
                manager.enter(player);
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("leave")) {
            DeepMineManager manager = resolveManager(player, args, 1);
            if (manager != null) {
                manager.leave(player);
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("info")) {
            handleInfo(player, args.length > 1 ? args[1] : null);
            return true;
        }
        if (args[0].equalsIgnoreCase("create")) {
            registry.createAutoInstance(player);
            return true;
        }
        player.sendMessage("§c사용법: /deepmine [enter|leave|info] [id] §7또는§c /deepmine create");
        return true;
    }

    private DeepMineManager resolveManager(Player player, String[] args, int idArgIndex) {
        if (args.length > idArgIndex) {
            DeepMineManager manager = registry.get(args[idArgIndex]);
            if (manager == null) {
                player.sendMessage("§c알 수 없는 딥 마인 인스턴스: " + args[idArgIndex]);
            }
            return manager;
        }
        if (registry.getAll().isEmpty()) {
            player.sendMessage("§c등록된 딥 마인 인스턴스가 없습니다. §7/deepmine create §c로 새로 만드세요.");
            return null;
        }
        return registry.getAll().get(0);
    }

    private void handleInfo(Player player, String id) {
        List<DeepMineManager> targets = id != null
                ? (registry.get(id) != null ? List.of(registry.get(id)) : List.of())
                : registry.getAll();

        if (targets.isEmpty()) {
            player.sendMessage("§c표시할 딥 마인 인스턴스가 없습니다.");
            return;
        }

        player.sendMessage("§6═══ 딥 마인 위치 정보 (" + targets.size() + "개) ═══");
        for (DeepMineManager manager : targets) {
            Location entrance = manager.getEntrance();
            player.sendMessage("§e▶ " + manager.getId());
            player.sendMessage("§7  월드: §f" + manager.getWorldName());
            player.sendMessage("§7  입구: §fX=" + entrance.getBlockX() + " Y=" + entrance.getBlockY() + " Z=" + entrance.getBlockZ());
            player.sendMessage("§7  경계: §f(" + manager.getMinX() + ", " + manager.getMinY() + ", " + manager.getMinZ()
                    + ") ~ (" + manager.getMaxX() + ", " + manager.getMaxY() + ", " + manager.getMaxZ() + ")");
        }
        player.sendMessage("§e/deepmine enter <id> §7로 즉시 입구로 이동할 수 있습니다.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("enter", "leave", "info", "create");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("enter") || args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("info"))) {
            List<String> ids = new ArrayList<>();
            for (DeepMineManager manager : registry.getAll()) {
                ids.add(manager.getId());
            }
            return ids;
        }
        return List.of();
    }
}
