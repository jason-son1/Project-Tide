package com.tide.core;

import com.tide.core.admin.AdminGUI;
import com.tide.core.admin.AdminListener;
import com.tide.core.death.DeathListener;
import com.tide.core.death.GraveInteractListener;
import com.tide.core.death.GraveManager;
import com.tide.core.economy.EconomyAPI;
import com.tide.core.economy.EconomyManager;
import com.tide.core.guide.GuideGUI;
import com.tide.core.guide.GuideListener;
import com.tide.core.guide.GuideRegistry;
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
    private com.tide.core.effect.EffectEngine effectEngine;
    private com.tide.core.resource.ResourcePackManager resourcePackManager;
    private GuideRegistry guideRegistry;

    private static final String[] SAMPLE_GUIDE_ENTRIES = {
            "tide_cycle", "tide_spring", "tide_bloodmoon",
            "forge_reinforce", "forge_socket", "forge_rune_effects",
            "mob_elite", "mob_nemesis", "mob_calamity", "mob_legacy_theft",
            "bounty_board", "bounty_contract",
            "death_grave", "death_currency", "death_hardcore",
            "ext_currents", "ext_resonance", "ext_overdrive",
            "ext_rune_awakening", "ext_biolum", "ext_gates"
    };


    public static TideCorePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getDataFolder().mkdirs();
        new java.io.File(getDataFolder(), "data").mkdirs();

        this.effectEngine = new com.tide.core.effect.EffectEngine(this);
        this.effectEngine.load();
        Bukkit.getServicesManager().register(com.tide.core.effect.EffectEngine.class, effectEngine, this, ServicePriority.Normal);

        this.resourcePackManager = new com.tide.core.resource.ResourcePackManager(this);
        this.resourcePackManager.load();
        getServer().getPluginManager().registerEvents(resourcePackManager, this);
        Bukkit.getServicesManager().register(com.tide.core.resource.ResourcePackManager.class, resourcePackManager, this, ServicePriority.Normal);

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
            this.effectEngine.load();
            this.resourcePackManager.load();
            return 1;
        });
        Bukkit.getServicesManager().register(ReloadManager.class, reloadManager, this, ServicePriority.Normal);

        AdminGUI adminGUI = new AdminGUI(this, tideScheduler, economyManager);
        getServer().getPluginManager().registerEvents(
                new AdminListener(adminGUI, tideScheduler, reloadManager, economyManager), this);

        extractBundledGuides();
        this.guideRegistry = new GuideRegistry(this);
        guideRegistry.reload();
        GuideGUI guideGUI = new GuideGUI(guideRegistry);
        getServer().getPluginManager().registerEvents(new GuideListener(guideGUI), this);
        reloadManager.register("guide", guideRegistry);

        // Main Menu GUI & Listener
        com.tide.core.menu.MainMenuGUI mainMenuGUI = new com.tide.core.menu.MainMenuGUI(economyManager);
        getServer().getPluginManager().registerEvents(
                new com.tide.core.menu.MainMenuListener(mainMenuGUI), this);

        getCommand("clam").setExecutor(new com.tide.core.commands.ClamCommand(economyManager));
        getCommand("pearl").setExecutor(new com.tide.core.commands.PearlCommand(economyManager));
        getCommand("tide").setExecutor(new com.tide.core.commands.TideCommand(tideScheduler, reloadManager, adminGUI));
        getCommand("hardcore").setExecutor(new com.tide.core.commands.HardcoreCommand(economyManager));
        getCommand("menu").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                mainMenuGUI.open(player);
                return true;
            } else {
                sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
                return false;
            }
        });
        getCommand("guide").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                guideGUI.openCategories(player);
                return true;
            } else {
                sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
                return false;
            }
        });

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

    public com.tide.core.effect.EffectEngine getEffectEngine() {
        return effectEngine;
    }

    public com.tide.core.resource.ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public GuideRegistry getGuideRegistry() {
        return guideRegistry;
    }

    private void extractBundledGuides() {
        for (String id : SAMPLE_GUIDE_ENTRIES) {
            java.io.File file = new java.io.File(getDataFolder(), "guide/" + id + ".yml");
            if (!file.exists()) {
                saveResource("guide/" + id + ".yml", false);
            }
        }
    }
}
