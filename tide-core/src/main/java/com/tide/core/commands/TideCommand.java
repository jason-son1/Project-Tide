package com.tide.core.commands;

import com.tide.core.admin.AdminGUI;
import com.tide.core.TideCorePlugin;
import com.tide.core.lobby.LobbyManager;
import com.tide.core.reload.ReloadManager;
import com.tide.core.tide.TideScheduler;
import com.tide.core.tide.TideState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class TideCommand implements CommandExecutor, TabCompleter {

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
            case "lobby" -> handleLobby(sender, args);
            case "addclam" -> handleAddClam(sender, args);
            case "addpearl" -> handleAddPearl(sender, args);
            case "addrep" -> handleAddRep(sender, args);
            case "giveitem" -> handleGiveItem(sender, args);
            default -> sender.sendMessage("§c사용법: /tide [reload|set|admin|tempo|lobby|addclam|addpearl|addrep|giveitem]");
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

    private void handleLobby(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tide.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§c사용법: /tide lobby [create | recreate | tp]");
            return;
        }

        LobbyManager lobbyManager = TideCorePlugin.getInstance().getLobbyManager();
        switch (args[1].toLowerCase()) {
            case "create" -> {
                player.sendMessage("§e하늘 위에 새로운 로비를 생성 중입니다...");
                lobbyManager.createLobby(player.getWorld());
                player.teleport(lobbyManager.getLobbySpawn());
                player.sendMessage("§a로비 생성이 완료되었으며 이동되었습니다.");
            }
            case "recreate" -> {
                player.sendMessage("§e기존 로비를 청소하고 새로운 로비를 생성 중입니다...");
                lobbyManager.recreateLobby(player.getWorld());
                player.teleport(lobbyManager.getLobbySpawn());
                player.sendMessage("§a로비가 새로 생성되었으며 이동되었습니다.");
            }
            case "tp" -> {
                if (lobbyManager.getLobbySpawn() == null) {
                    player.sendMessage("§c생성된 로비가 없습니다. /tide lobby create로 먼저 생성하세요.");
                    return;
                }
                player.teleport(lobbyManager.getLobbySpawn());
                player.sendMessage("§a로비로 텔레포트했습니다.");
            }
            default -> player.sendMessage("§c알 수 없는 하위 명령어: /tide lobby [create | recreate | tp]");
        }
    }

    private void handleAddClam(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tide.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /tide addclam <플레이어> <액수>");
            return;
        }
        Player target = org.bukkit.Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[1]);
            return;
        }
        try {
            long amount = Long.parseLong(args[2]);
            com.tide.core.economy.EconomyAPI economyAPI = org.bukkit.Bukkit.getServicesManager().load(com.tide.core.economy.EconomyAPI.class);
            if (economyAPI == null) {
                sender.sendMessage("§cEconomyAPI 서비스를 찾을 수 없습니다.");
                return;
            }
            economyAPI.addClam(target.getUniqueId(), amount);
            sender.sendMessage("§a" + target.getName() + "님에게 §6조개 " + amount + "개§a를 지급했습니다.");
            target.sendMessage("§a관리자가 귀하에게 §6조개 " + amount + "개§a를 지급했습니다.");
        } catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 금액을 입력해주세요.");
        }
    }

    private void handleAddPearl(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tide.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /tide addpearl <플레이어> <액수>");
            return;
        }
        Player target = org.bukkit.Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[1]);
            return;
        }
        try {
            long amount = Long.parseLong(args[2]);
            com.tide.core.economy.EconomyAPI economyAPI = org.bukkit.Bukkit.getServicesManager().load(com.tide.core.economy.EconomyAPI.class);
            if (economyAPI == null) {
                sender.sendMessage("§cEconomyAPI 서비스를 찾을 수 없습니다.");
                return;
            }
            economyAPI.addPearl(target.getUniqueId(), amount);
            sender.sendMessage("§a" + target.getName() + "님에게 §d진주 " + amount + "개§a를 지급했습니다.");
            target.sendMessage("§a관리자가 귀하에게 §d진주 " + amount + "개§a를 지급했습니다.");
        } catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 금액을 입력해주세요.");
        }
    }

    private void handleAddRep(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tide.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /tide addrep <플레이어> <액수>");
            return;
        }
        Player target = org.bukkit.Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[1]);
            return;
        }
        try {
            int amount = Integer.parseInt(args[2]);
            com.tide.core.economy.EconomyAPI economyAPI = org.bukkit.Bukkit.getServicesManager().load(com.tide.core.economy.EconomyAPI.class);
            if (economyAPI == null) {
                sender.sendMessage("§cEconomyAPI 서비스를 찾을 수 없습니다.");
                return;
            }
            economyAPI.addRep(target.getUniqueId(), amount);
            sender.sendMessage("§a" + target.getName() + "님에게 §e평판 " + amount + "§a을 지급했습니다.");
            target.sendMessage("§a관리자가 귀하에게 §e평판 " + amount + "§a을 지급했습니다.");
        } catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 숫자를 입력해주세요.");
        }
    }

    private void handleGiveItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tide.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /tide giveitem <플레이어> <아이템ID> [수량]");
            return;
        }
        Player target = org.bukkit.Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[1]);
            return;
        }
        String itemId = args[2].toLowerCase();
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) amount = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage("§c올바른 수량을 입력해주세요. 기본값인 1개로 지급합니다.");
            }
        }

        try {
            Class<?> itemFactoryClass = Class.forName("com.tide.rpg.item.ItemFactory");
            Object itemFactoryObj = org.bukkit.Bukkit.getServicesManager().load(itemFactoryClass);
            if (itemFactoryObj == null) {
                sender.sendMessage("§cItemFactory 서비스가 등록되지 않았습니다. TideRPG 플러그인이 비활성화 상태이거나 로드되지 않았습니다.");
                return;
            }

            java.lang.reflect.Method createMethod = itemFactoryClass.getMethod("create", String.class);
            org.bukkit.inventory.ItemStack item = (org.bukkit.inventory.ItemStack) createMethod.invoke(itemFactoryObj, itemId);

            item.setAmount(amount);
            target.getInventory().addItem(item);
            sender.sendMessage("§a" + target.getName() + "님에게 " + item.getItemMeta().getDisplayName() + " §f" + amount + "개§a를 지급했습니다.");
            target.sendMessage("§a관리자가 귀하에게 " + item.getItemMeta().getDisplayName() + " §f" + amount + "개§a를 지급했습니다.");
        } catch (ClassNotFoundException e) {
            sender.sendMessage("§cTideRPG 플러그인이 활성화되어 있지 않습니다.");
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                sender.sendMessage("§c존재하지 않는 커스텀 아이템 ID입니다: " + itemId);
            } else {
                sender.sendMessage("§c아이템 생성 중 오류가 발생했습니다: " + e.getCause().getMessage());
            }
        } catch (Exception e) {
            sender.sendMessage("§c리플렉션 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("tide.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = List.of("reload", "set", "admin", "tempo", "lobby", "addclam", "addpearl", "addrep", "giveitem");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "reload" -> {
                    List<String> targets = new ArrayList<>(reloadManager.getReloadableNames());
                    StringUtil.copyPartialMatches(args[1], targets, completions);
                    Collections.sort(completions);
                    return completions;
                }
                case "set" -> {
                    List<String> states = List.of("HIGH_TIDE", "LOW_TIDE", "SPRING_TIDE", "BLOOD_MOON", "BLOOD_TIDE");
                    StringUtil.copyPartialMatches(args[1], states, completions);
                    Collections.sort(completions);
                    return completions;
                }
                case "lobby" -> {
                    List<String> lobbyActions = List.of("create", "recreate", "tp");
                    StringUtil.copyPartialMatches(args[1], lobbyActions, completions);
                    Collections.sort(completions);
                    return completions;
                }
                case "addclam", "addpearl", "addrep", "giveitem" -> {
                    List<String> playerNames = new ArrayList<>();
                    for (Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                        playerNames.add(onlinePlayer.getName());
                    }
                    StringUtil.copyPartialMatches(args[1], playerNames, completions);
                    Collections.sort(completions);
                    return completions;
                }
            }
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("giveitem")) {
                try {
                    Class<?> itemFactoryClass = Class.forName("com.tide.rpg.item.ItemFactory");
                    Object itemFactoryObj = org.bukkit.Bukkit.getServicesManager().load(itemFactoryClass);
                    if (itemFactoryObj != null) {
                        java.lang.reflect.Method getRegisteredIdsMethod = itemFactoryClass.getMethod("getRegisteredIds");
                        @SuppressWarnings("unchecked")
                        java.util.Collection<String> ids = (java.util.Collection<String>) getRegisteredIdsMethod.invoke(itemFactoryObj);
                        List<String> itemIds = new ArrayList<>(ids);
                        StringUtil.copyPartialMatches(args[2], itemIds, completions);
                        Collections.sort(completions);
                        return completions;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return Collections.emptyList();
    }
}
