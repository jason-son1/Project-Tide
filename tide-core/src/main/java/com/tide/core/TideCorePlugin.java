package com.tide.core;

import com.tide.core.admin.AdminGUI;
import com.tide.core.admin.AdminListener;
import com.tide.core.death.DeathListener;
import com.tide.core.death.GraveInteractListener;
import com.tide.core.death.GraveManager;
import com.tide.core.economy.EconomyAPI;
import com.tide.core.economy.EconomyManager;
import com.tide.core.hud.EconomyScoreboardHud;
import com.tide.core.hud.HudJoinListener;
import com.tide.core.reload.ReloadManager;
import com.tide.core.tide.TideScheduler;
import com.tide.core.tide.TideStateProvider;
import com.tide.core.web.TideWebServer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class TideCorePlugin extends JavaPlugin {

    private static TideCorePlugin instance;

    private EconomyManager economyManager;
    private TideScheduler tideScheduler;
    private GraveManager graveManager;
    private EconomyScoreboardHud economyScoreboardHud;
    private TideWebServer webServer;
    private final ReloadManager reloadManager = new ReloadManager();


    public static TideCorePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getDataFolder().mkdirs();
        new java.io.File(getDataFolder(), "data").mkdirs();

        this.economyManager = new EconomyManager(this);
        this.economyManager.init();
        Bukkit.getServicesManager().register(EconomyAPI.class, economyManager, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(economyManager, this);

        this.tideScheduler = new TideScheduler(this);
        this.tideScheduler.start();
        Bukkit.getServicesManager().register(TideStateProvider.class, tideScheduler, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(new com.tide.core.tide.TideBossBarListener(tideScheduler), this);

        this.graveManager = new GraveManager(this);
        graveManager.start();
        getServer().getPluginManager().registerEvents(new DeathListener(this, economyManager, graveManager), this);
        getServer().getPluginManager().registerEvents(new GraveInteractListener(graveManager), this);

        this.economyScoreboardHud = new EconomyScoreboardHud(this, economyManager, graveManager);
        economyScoreboardHud.start();
        getServer().getPluginManager().registerEvents(new HudJoinListener(economyScoreboardHud), this);
        for (var player : Bukkit.getOnlinePlayers()) {
            economyScoreboardHud.setup(player);
        }

        reloadManager.register("config", () -> {
            reloadConfig();
            return 1;
        });
        Bukkit.getServicesManager().register(ReloadManager.class, reloadManager, this, ServicePriority.Normal);

        AdminGUI adminGUI = new AdminGUI(this, tideScheduler, economyManager);
        getServer().getPluginManager().registerEvents(
                new AdminListener(adminGUI, tideScheduler, reloadManager, economyManager), this);

        getCommand("clam").setExecutor(new com.tide.core.commands.ClamCommand(economyManager));
        getCommand("pearl").setExecutor(new com.tide.core.commands.PearlCommand(economyManager));
        getCommand("tide").setExecutor(new com.tide.core.commands.TideCommand(tideScheduler, reloadManager, adminGUI));
        getCommand("hardcore").setExecutor(new com.tide.core.commands.HardcoreCommand(economyManager));

        boolean webEnabled = getConfig().getBoolean("web-server.enabled", true);
        int webPort = getConfig().getInt("web-server.port", 8080);
        if (webEnabled) {
            this.webServer = new TideWebServer(this, webPort);
            this.webServer.start();
        }

        getLogger().info("TideCore enabled.");
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
        if (tideScheduler != null) {
            tideScheduler.stop();
        }
        if (economyScoreboardHud != null) {
            economyScoreboardHud.stop();
        }
        if (graveManager != null) {
            graveManager.stop();
        }
        if (economyManager != null) {
            economyManager.shutdown();
        }
        Bukkit.getServicesManager().unregisterAll(this);
        getLogger().info("TideCore disabled.");
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public TideScheduler getTideScheduler() {
        return tideScheduler;
    }

    public ReloadManager getReloadManager() {
        return reloadManager;
    }

    public GraveManager getGraveManager() {
        return graveManager;
    }
}
