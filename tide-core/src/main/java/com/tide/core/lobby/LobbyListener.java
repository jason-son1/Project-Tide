package com.tide.core.lobby;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

public class LobbyListener implements Listener {

    private final LobbyManager lobbyManager;

    public LobbyListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // 1. Cancel pending teleports if player moves block location
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            lobbyManager.cancelTeleport(player, true);
        }

        // 2. Prevent falling into the void from the lobby
        if (lobbyManager.getLobbySpawn() != null && lobbyManager.isInsideLobby(player.getLocation())) {
            if (player.getLocation().getY() < lobbyManager.getLobbySpawn().getY() - 3.0) {
                player.teleport(lobbyManager.getLobbySpawn());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendMessage("§c로비 구역 밖으로 나갈 수 없습니다!");
            }
        }

        // 3. Escape portal block step-on check
        if (lobbyManager.getLobbySpawn() != null && lobbyManager.isInsideLobby(player.getLocation())) {
            Location playerLoc = player.getLocation();
            org.bukkit.block.Block blockUnder = playerLoc.clone().subtract(0, 0.1, 0).getBlock();
            if (blockUnder.getType() == Material.LIGHT_BLUE_STAINED_GLASS) {
                int cz = lobbyManager.getLobbySpawn().getBlockZ();
                if (Math.abs(blockUnder.getZ() - (cz - 11)) <= 1) {
                    teleportToSpawn(player);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Protect players inside the lobby from taking any damage (fall damage, void damage, hunger, pvp, etc.)
        if (event.getEntity() instanceof Player player) {
            if (lobbyManager.isInsideLobby(player.getLocation())) {
                event.setCancelled(true);
            } else {
                // Cancel pending lobby teleport if they take damage outside lobby
                lobbyManager.cancelTeleport(player, true);
            }
        }

        // Protect the Lobby NPC
        if (event.getEntity().getPersistentDataContainer().has(lobbyManager.getNpcKey(), PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Protect players inside the lobby from PVP/mobs
        if (event.getEntity() instanceof Player player) {
            if (lobbyManager.isInsideLobby(player.getLocation())) {
                event.setCancelled(true);
            }
        }

        // Handle NPC left click
        if (event.getEntity().getPersistentDataContainer().has(lobbyManager.getNpcKey(), PersistentDataType.BYTE)) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
                teleportToSpawn(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Handle NPC right click
        if (event.getRightClicked().getPersistentDataContainer().has(lobbyManager.getNpcKey(), PersistentDataType.BYTE)) {
            event.setCancelled(true);
            teleportToSpawn(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        // Handle NPC right click at position
        if (event.getRightClicked().getPersistentDataContainer().has(lobbyManager.getNpcKey(), PersistentDataType.BYTE)) {
            event.setCancelled(true);
            teleportToSpawn(event.getPlayer());
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (lobbyManager.isInsideLobby(event.getClickedBlock().getLocation())) {
            org.bukkit.Material mat = event.getClickedBlock().getType();
            if (mat.name().contains("BUTTON") || mat.name().contains("SIGN")) {
                Player player = event.getPlayer();
                if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
                    return;
                }
                event.setCancelled(true);
                teleportToSpawn(player);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (lobbyManager.isInsideLobby(event.getBlock().getLocation())) {
            // Bypass protection for OP players in Creative mode
            if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage("§c로비 구역의 블록을 파괴할 수 없습니다.");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (lobbyManager.isInsideLobby(event.getBlock().getLocation())) {
            // Bypass protection for OP players in Creative mode
            if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage("§c로비 구역에 블록을 설치할 수 없습니다.");
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (lobbyManager.isInsideLobby(player.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (lobbyManager.isInsideLobby(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (lobbyManager.isInsideLobby(player.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (lobbyManager.isInsideLobby(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (lobbyManager.isInsideLobby(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lobbyManager.cancelTeleport(event.getPlayer(), false);
    }

    private void teleportToSpawn(Player player) {
        Location spawnLoc = player.getRespawnLocation();
        if (spawnLoc == null) {
            spawnLoc = player.getWorld().getSpawnLocation();
        }
        player.teleport(spawnLoc);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.5, 1.0, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 15, 0.5, 1.0, 0.5, 0.05);
        player.sendMessage("§a스폰 지점으로 이동했습니다!");
    }
}
