package com.tide.mobs;

import com.tide.core.economy.EconomyAPI;
import com.tide.core.reload.ReloadManager;
import com.tide.core.tide.TideStateProvider;
import com.tide.mobs.affix.AffixCombatListener;
import com.tide.mobs.affix.AffixRegistry;
import com.tide.mobs.affix.EliteDropListener;
import com.tide.mobs.affix.EliteProcessor;
import com.tide.mobs.affix.EliteSpawnListener;
import com.tide.mobs.boss.AltarInteractListener;
import com.tide.mobs.boss.AltarRegistry;
import com.tide.mobs.boss.BossCombatListener;
import com.tide.mobs.boss.BossFightManager;
import com.tide.mobs.nemesis.NemesisManager;
import com.tide.mobs.nemesis.NemesisRewardListener;
import com.tide.mobs.nemesis.NemesisTracker;
import com.tide.mobs.nemesis.NemesisTriggerListener;
import com.tide.mobs.quest.BountyBoardGUI;
import com.tide.mobs.quest.BountyBoardListener;
import com.tide.mobs.quest.BountyKillListener;
import com.tide.mobs.quest.BountyManager;
import com.tide.rpg.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class TideMobsPlugin extends JavaPlugin {

    // Filenames are ASCII (jar entry names can get mangled by platform charset
    // on non-UTF8 locales); the Korean affix id itself lives in each yml's "id:" field.
    private static final String[] SAMPLE_AFFIXES = {
            "flame", "haste", "explosive", "split", "iron", "regen", "thorns", "shield"
    };
    private static final String[] SAMPLE_ALTARS = {
            "void_knight_altar"
    };

    private AffixRegistry affixRegistry;
    private AltarRegistry altarRegistry;
    private BountyManager bountyManager;
    private BountyBoardGUI bountyBoardGUI;
    private NemesisManager nemesisManager;
    private NemesisTracker nemesisTracker;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        TideStateProvider tideStateProvider = lookupService(TideStateProvider.class);
        EconomyAPI economyAPI = lookupService(EconomyAPI.class);
        if (tideStateProvider == null || economyAPI == null) {
            getLogger().severe("TideCore의 서비스(TideStateProvider/EconomyAPI)를 찾을 수 없습니다. TideCore가 먼저 로드되어야 합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ItemFactory itemFactory = lookupService(ItemFactory.class);

        extractBundled();

        this.affixRegistry = new AffixRegistry(this);
        this.altarRegistry = new AltarRegistry(this);
        affixRegistry.reload();
        altarRegistry.reload();

        EliteProcessor eliteProcessor = new EliteProcessor();
        getServer().getPluginManager().registerEvents(
                new EliteSpawnListener(tideStateProvider, affixRegistry, eliteProcessor), this);
        getServer().getPluginManager().registerEvents(new AffixCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new EliteDropListener(itemFactory), this);

        BossFightManager bossFightManager = new BossFightManager(this, economyAPI);
        bossFightManager.start();
        getServer().getPluginManager().registerEvents(new AltarInteractListener(altarRegistry, bossFightManager), this);
        getServer().getPluginManager().registerEvents(new BossCombatListener(bossFightManager), this);

        this.bountyManager = new BountyManager(affixRegistry, economyAPI);
        this.bountyBoardGUI = new BountyBoardGUI(bountyManager);
        getServer().getPluginManager().registerEvents(new BountyKillListener(bountyManager), this);
        getServer().getPluginManager().registerEvents(new BountyBoardListener(bountyManager, bountyBoardGUI), this);

        this.nemesisManager = new NemesisManager(this);
        nemesisManager.init();
        getServer().getPluginManager().registerEvents(new NemesisTriggerListener(this, nemesisManager), this);
        getServer().getPluginManager().registerEvents(new NemesisRewardListener(nemesisManager, economyAPI, itemFactory), this);
        this.nemesisTracker = new NemesisTracker(this, nemesisManager);
        nemesisTracker.start();

        ReloadManager reloadManager = lookupService(ReloadManager.class);
        if (reloadManager != null) {
            reloadManager.register("affixes", affixRegistry);
            reloadManager.register("altars", altarRegistry);
        }

        getCommand("bounty").setExecutor(this);

        getLogger().info("TideMobs enabled. 접두사 " + affixRegistry.all().size() + "개 로드.");
    }

    @Override
    public void onDisable() {
        if (nemesisTracker != null) {
            nemesisTracker.stop();
        }
        if (nemesisManager != null) {
            nemesisManager.shutdown();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("bounty") && sender instanceof Player player) {
            bountyBoardGUI.open(player);
            return true;
        }
        return false;
    }

    private void extractBundled() {
        for (String id : SAMPLE_AFFIXES) {
            saveResource("affixes/" + id + ".yml", false);
        }
        for (String id : SAMPLE_ALTARS) {
            saveResource("altars/" + id + ".yml", false);
        }
    }

    private <T> T lookupService(Class<T> serviceClass) {
        RegisteredServiceProvider<T> provider = Bukkit.getServicesManager().getRegistration(serviceClass);
        return provider == null ? null : provider.getProvider();
    }
}
