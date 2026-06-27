package com.tide.mobs.boss;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * /altar create <id> [boss_type] [required_fragments] [party_size]
 * /altar list
 * /altar info <id>
 * /altar remove <id>
 *
 * Requires tide.admin permission.
 */
public final class AltarCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final AltarRegistry altarRegistry;

    public AltarCommand(JavaPlugin plugin, AltarRegistry altarRegistry) {
        this.plugin = plugin;
        this.altarRegistry = altarRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!player.hasPermission("tide.admin")) {
            player.sendMessage("§c권한이 없습니다. (tide.admin 필요)");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "list"   -> handleList(player);
            case "info"   -> handleInfo(player, args);
            case "remove" -> handleRemove(player, args);
            default       -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c사용법: /altar create <id> [boss_type] [필요파편수] [권장인원]");
            return;
        }
        String id = args[1];
        String bossType = args.length >= 3 ? args[2].toUpperCase() : "VOID_KNIGHT";
        int fragments = args.length >= 4 ? parseIntSafe(args[3], 5) : 5;
        int partySize = args.length >= 5 ? parseIntSafe(args[4], 3) : 3;

        // Validate boss type
        if (!bossType.equals("VOID_KNIGHT") && !bossType.equals("CORAL_QUEEN") && !bossType.equals("ABYSSAL_TITAN")) {
            player.sendMessage("§c알 수 없는 보스 타입: " + bossType);
            player.sendMessage("§7사용 가능: VOID_KNIGHT, CORAL_QUEEN, ABYSSAL_TITAN");
            return;
        }

        Location loc = player.getLocation().getBlock().getLocation();
        String worldName = loc.getWorld().getName();

        File altarsDir = new File(plugin.getDataFolder(), "altars");
        altarsDir.mkdirs();
        File outFile = new File(altarsDir, id + ".yml");
        if (outFile.exists()) {
            player.sendMessage("§c해당 ID의 제단이 이미 존재합니다: " + id);
            return;
        }

        // Determine boss display name
        String displayName = switch (bossType) {
            case "CORAL_QUEEN"   -> "§3§l산호 여왕";
            case "ABYSSAL_TITAN" -> "§5§l심연의 거신";
            default              -> "§4§l공허의 기사";
        };

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", id);
        yaml.set("world", worldName);
        yaml.set("block.x", loc.getBlockX());
        yaml.set("block.y", loc.getBlockY());
        yaml.set("block.z", loc.getBlockZ());
        yaml.set("required_fragments", fragments);
        yaml.set("recommended_party_size", partySize);
        yaml.set("boss_type", bossType);
        yaml.set("boss_display_name", displayName);

        try {
            yaml.save(outFile);
        } catch (IOException e) {
            player.sendMessage("§c제단 파일 저장 실패: " + e.getMessage());
            return;
        }

        // Reload registry
        altarRegistry.reload();

        // Build the altar structure
        AltarBuilder.build(loc);

        player.sendMessage("§a[제단 생성] §f'" + id + "' 제단을 현재 위치에 생성했습니다.");
        player.sendMessage("§7보스 타입: §f" + bossType + "  §7필요 파편: §f" + fragments + "  §7권장 인원: §f" + partySize + "명");
        player.sendMessage("§7제단 외형이 주변에 건축되었습니다.");
    }

    private void handleList(Player player) {
        var altars = altarRegistry.getAll();
        if (altars.isEmpty()) {
            player.sendMessage("§7등록된 제단이 없습니다. /altar create <id>로 생성하세요.");
            return;
        }
        player.sendMessage("§6═══ 등록된 제단 목록 (" + altars.size() + "개) ═══");
        for (SoulAltar altar : altars) {
            player.sendMessage("§e• §f" + altar.getId()
                    + "  §7보스: " + altar.getBossType()
                    + "  §7위치: " + altar.getWorld() + " [" + altar.getBlockX() + "," + altar.getBlockY() + "," + altar.getBlockZ() + "]");
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c사용법: /altar info <id>");
            return;
        }
        SoulAltar altar = altarRegistry.findById(args[1]);
        if (altar == null) {
            player.sendMessage("§c해당 ID의 제단을 찾을 수 없습니다: " + args[1]);
            return;
        }
        player.sendMessage("§6═══ 제단 정보: " + altar.getId() + " ═══");
        player.sendMessage("§7보스 타입: §f" + altar.getBossType());
        player.sendMessage("§7보스 이름: " + altar.getBossDisplayName());
        player.sendMessage("§7위치: §f" + altar.getWorld() + " §7X=" + altar.getBlockX() + " Y=" + altar.getBlockY() + " Z=" + altar.getBlockZ());
        player.sendMessage("§7필요 영혼 파편: §f" + altar.getRequiredFragments() + "개");
        player.sendMessage("§7권장 인원: §f" + altar.getRecommendedPartySize() + "명");
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c사용법: /altar remove <id>");
            return;
        }
        String id = args[1];
        File altarsDir = new File(plugin.getDataFolder(), "altars");
        File targetFile = new File(altarsDir, id + ".yml");
        if (!targetFile.exists()) {
            player.sendMessage("§c해당 ID의 제단 파일이 없습니다: " + id);
            return;
        }
        boolean deleted = targetFile.delete();
        if (deleted) {
            altarRegistry.reload();
            player.sendMessage("§a[제단 삭제] §f'" + id + "' 제단이 삭제되었습니다.");
        } else {
            player.sendMessage("§c파일 삭제에 실패했습니다.");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6━━━ 보스 제단 명령어 ━━━");
        player.sendMessage("§e/altar create <id> [boss_type] [파편수] [인원] §7— 현재 위치에 제단 생성 및 건축");
        player.sendMessage("§e/altar list                                 §7— 등록된 제단 목록");
        player.sendMessage("§e/altar info <id>                            §7— 제단 상세 정보");
        player.sendMessage("§e/altar remove <id>                          §7— 제단 삭제");
        player.sendMessage("§7보스 타입: §fVOID_KNIGHT §7/ §fCORAL_QUEEN §7/ §fABYSSAL_TITAN");
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "list", "info", "remove");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return List.of("VOID_KNIGHT", "CORAL_QUEEN", "ABYSSAL_TITAN");
        }
        return List.of();
    }
}
