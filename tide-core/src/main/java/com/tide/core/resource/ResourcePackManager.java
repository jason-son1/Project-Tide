package com.tide.core.resource;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourcePackManager implements Listener {

    private final JavaPlugin plugin;
    
    private boolean enabled;
    private String url;
    private String hash;
    private boolean force;
    private String prompt;

    public ResourcePackManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("resource-pack");
        if (sec == null) {
            this.enabled = false;
            return;
        }
        this.enabled = sec.getBoolean("enabled", false);
        this.url = sec.getString("url", "");
        this.hash = sec.getString("hash", "");
        this.force = sec.getBoolean("force", false);
        this.prompt = sec.getString("prompt", "Project Tide 전용 리소스팩을 다운로드합니다.");
    }

    public void applyPack(Player player) {
        if (!enabled || url.isEmpty()) {
            return;
        }

        byte[] hashBytes = null;
        if (hash != null && !hash.isEmpty()) {
            try {
                hashBytes = hexStringToByteArray(hash);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse resource-pack hash hex: " + hash);
            }
        }

        String promptMsg = ChatColor.translateAlternateColorCodes('&', prompt);

        try {
            if (hashBytes != null) {
                player.setResourcePack(url, hashBytes, promptMsg, force);
            } else {
                player.setResourcePack(url, new byte[0], promptMsg, force);
            }
        } catch (NoSuchMethodError | Exception e) {
            try {
                player.setResourcePack(url);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to set resource pack for player: " + player.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                applyPack(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (!enabled || !force) {
            return;
        }

        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        if (status == PlayerResourcePackStatusEvent.Status.DECLINED) {
            player.kickPlayer("§cProject Tide 리소스팩 적용을 거부하셨습니다.\n§7서버 플레이를 위해 리소스팩 다운로드를 수락해주세요.");
        } else if (status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            player.sendMessage("§c리소스팩 다운로드에 실패했습니다. 재접속을 권장합니다.");
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
