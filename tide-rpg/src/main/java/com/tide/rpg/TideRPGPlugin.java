package com.tide.rpg;

import java.io.File;
import com.tide.core.economy.EconomyAPI;
import com.tide.core.reload.ReloadManager;
import com.tide.core.tide.TideStateProvider;
import com.tide.rpg.combat.CombatListener;
import com.tide.rpg.combat.DefensiveListener;
import com.tide.rpg.combat.RuneEffectDispatcher;
import com.tide.rpg.deepmine.DeepMineCommand;
import com.tide.rpg.deepmine.DeepMineListener;
import com.tide.rpg.deepmine.DeepMineManager;
import com.tide.rpg.fishing.FishingHoleRegistry;
import com.tide.rpg.fishing.FishingQteListener;
import com.tide.rpg.forge.ForgeGUI;
import com.tide.rpg.forge.ForgeListener;
import com.tide.rpg.gs.GearScoreCalculator;
import com.tide.rpg.item.ItemFactory;
import com.tide.rpg.item.ItemRegistry;
import com.tide.rpg.item.LoreRenderer;
import com.tide.rpg.item.TideBellListener;
import com.tide.rpg.item.TideVaultListener;
import com.tide.rpg.rune.RuneItemFactory;
import com.tide.rpg.rune.RuneRegistry;
import com.tide.rpg.sell.SellAllCommand;
import com.tide.rpg.sell.SellAllManager;
import com.tide.rpg.shop.ShopGUI;
import com.tide.rpg.tideext.BioluminescentHarvestListener;
import com.tide.rpg.tideext.BioluminescentManager;
import com.tide.rpg.tideext.GateRegistry;
import com.tide.rpg.tideext.OverdriveListener;
import com.tide.rpg.tideext.OverdriveManager;
import com.tide.rpg.tideext.ResonanceListener;
import com.tide.rpg.tideext.TidalCurrentManager;
import com.tide.rpg.tideext.TidalGateListener;
import com.tide.rpg.shop.ShopListener;
import com.tide.rpg.zone.ZoneGuardListener;
import com.tide.rpg.zone.ZoneRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class TideRPGPlugin extends JavaPlugin {

    private static final String[] SAMPLE_ITEMS = {
            "iron_sword_t1", "flame_sword_t1", "leather_armor_t1", "reinforce_stone", "protection_scroll",
            "soul_fragment", "nemesis_token", "tide_bell",
            "tide_resonance_blade", "bloodmoon_resonance_armor", "anchor_boots",
            "tide_extractor", "bioluminescent_essence"
    };
    private static final String[] SAMPLE_RUNES = {
            "rune_lifesteal_1", "rune_lifesteal_2", "rune_lightning_1"
    };
    private static final String[] SAMPLE_ZONES = {
            "deep_mine", "boss_arena"
    };
    private static final String[] SAMPLE_FISHING_HOLES = {
            "mythic_fishing_hole"
    };
    private static final String[] SAMPLE_GATES = {
            "sunken_ruins_gate"
    };

    private ItemRegistry itemRegistry;
    private RuneRegistry runeRegistry;
    private ZoneRegistry zoneRegistry;
    private FishingHoleRegistry fishingHoleRegistry;
    private ItemFactory itemFactory;
    private ShopGUI shopGUI;
    private ForgeGUI forgeGUI;
    private DeepMineManager deepMineManager;
    private GearScoreCalculator gearScoreCalculator;
    private TidalCurrentManager tidalCurrentManager;
    private BioluminescentManager bioluminescentManager;
    private GateRegistry gateRegistry;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();

        EconomyAPI economyAPI = lookupService(EconomyAPI.class);
        TideStateProvider tideStateProvider = lookupService(TideStateProvider.class);
        if (economyAPI == null || tideStateProvider == null) {
            getLogger().severe("TideCore의 서비스(EconomyAPI/TideStateProvider)를 찾을 수 없습니다. TideCore가 먼저 로드되어야 합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        extractBundledSamples();

        this.itemRegistry = new ItemRegistry(this);
        this.runeRegistry = new RuneRegistry(this);
        this.zoneRegistry = new ZoneRegistry(this);
        this.fishingHoleRegistry = new FishingHoleRegistry(this);
        itemRegistry.reload();
        runeRegistry.reload();
        zoneRegistry.reload();
        fishingHoleRegistry.reload();

        LoreRenderer loreRenderer = new LoreRenderer(itemRegistry, runeRegistry);
        this.itemFactory = new ItemFactory(itemRegistry, loreRenderer);
        RuneItemFactory runeItemFactory = new RuneItemFactory(runeRegistry);
        this.shopGUI = new ShopGUI(itemFactory);
        this.forgeGUI = new ForgeGUI();
        this.gearScoreCalculator = new GearScoreCalculator(getConfig().getDouble("gs.per_reinforce_star", 5));

        // Exposed so TideMobs can grant Tide items (e.g. soul fragments) on elite kills
        // without depending on TideRPGPlugin directly.
        Bukkit.getServicesManager().register(ItemFactory.class, itemFactory, this, org.bukkit.plugin.ServicePriority.Normal);
        Bukkit.getServicesManager().register(GearScoreCalculator.class, gearScoreCalculator, this, org.bukkit.plugin.ServicePriority.Normal);

        RuneEffectDispatcher dispatcher = new RuneEffectDispatcher(tideStateProvider);
        getServer().getPluginManager().registerEvents(new CombatListener(dispatcher), this);
        getServer().getPluginManager().registerEvents(new DefensiveListener(dispatcher), this);
        getServer().getPluginManager().registerEvents(new ShopListener(economyAPI, itemFactory), this);
        getServer().getPluginManager().registerEvents(
                new ForgeListener(this, economyAPI, loreRenderer, runeRegistry, runeItemFactory, forgeGUI), this);
        getServer().getPluginManager().registerEvents(
                new ZoneGuardListener(zoneRegistry, gearScoreCalculator), this);
        getServer().getPluginManager().registerEvents(
                new FishingQteListener(fishingHoleRegistry, economyAPI), this);
        getServer().getPluginManager().registerEvents(new TideBellListener(), this);
        getServer().getPluginManager().registerEvents(new TideVaultListener(itemFactory), this);

        // 심화 확장 기능 백서 Phase 1: Tide Resonance Set + Tidal Overdrive + Tidal Currents
        getServer().getPluginManager().registerEvents(new ResonanceListener(tideStateProvider), this);
        OverdriveManager overdriveManager = new OverdriveManager();
        getServer().getPluginManager().registerEvents(new OverdriveListener(overdriveManager, tideStateProvider), this);
        setupTidalCurrents(tideStateProvider);

        this.bioluminescentManager = new BioluminescentManager(this, tideStateProvider);
        bioluminescentManager.start();
        getServer().getPluginManager().registerEvents(
                new BioluminescentHarvestListener(bioluminescentManager, itemFactory), this);

        this.gateRegistry = new GateRegistry(this);
        gateRegistry.reload();
        TidalGateListener tidalGateListener = new TidalGateListener(gateRegistry);
        getServer().getPluginManager().registerEvents(tidalGateListener, this);
        tidalGateListener.applyAll(tideStateProvider.getCurrentState());

        setupDeepMine();

        ReloadManager reloadManager = lookupService(ReloadManager.class);
        if (reloadManager != null) {
            reloadManager.register("items", itemRegistry);
            reloadManager.register("runes", runeRegistry);
            reloadManager.register("zones", zoneRegistry);
            reloadManager.register("fishingholes", fishingHoleRegistry);
            reloadManager.register("gates", gateRegistry);
        }

        getCommand("shop").setExecutor(this);
        getCommand("forge").setExecutor(this);
        getCommand("deepmine").setExecutor(new DeepMineCommand(deepMineManager));
        getCommand("sellall").setExecutor(new SellAllCommand(new SellAllManager(itemRegistry, economyAPI)));

        getLogger().info("TideRPG enabled. 아이템 " + itemRegistry.getAll().size()
                + "개, 룬 " + runeRegistry.getAll().size() + "개, 구역 " + "로드 완료.");
    }

    @Override
    public void onDisable() {
        if (deepMineManager != null) {
            deepMineManager.stop();
        }
        if (tidalCurrentManager != null) {
            tidalCurrentManager.stop();
        }
        if (bioluminescentManager != null) {
            bioluminescentManager.stop();
        }
    }

    private void setupTidalCurrents(TideStateProvider tideStateProvider) {
        long transitionWindow = getConfig().getLong("tidal_currents.transition-window-seconds", 20);
        double strength = getConfig().getDouble("tidal_currents.strength", 0.25);
        double shoreX = getConfig().getDouble("tidal_currents.shore-direction.x", 1.0);
        double shoreZ = getConfig().getDouble("tidal_currents.shore-direction.z", 0.0);
        this.tidalCurrentManager = new TidalCurrentManager(this, tideStateProvider, transitionWindow, strength,
                new org.bukkit.util.Vector(shoreX, 0, shoreZ));
        tidalCurrentManager.start();
    }

    private void setupDeepMine() {
        String world = getConfig().getString("deepmine.world", "world");
        int minX = getConfig().getInt("deepmine.bounds.min.x");
        int minY = getConfig().getInt("deepmine.bounds.min.y");
        int minZ = getConfig().getInt("deepmine.bounds.min.z");
        int maxX = getConfig().getInt("deepmine.bounds.max.x");
        int maxY = getConfig().getInt("deepmine.bounds.max.y");
        int maxZ = getConfig().getInt("deepmine.bounds.max.z");
        double entranceX = getConfig().getDouble("deepmine.entrance.x");
        double entranceY = getConfig().getDouble("deepmine.entrance.y");
        double entranceZ = getConfig().getDouble("deepmine.entrance.z");
        long resetMinutes = getConfig().getLong("deepmine.reset-interval-minutes", 30);

        World bukkitWorld = Bukkit.getWorld(world);
        Location entrance = new Location(bukkitWorld, entranceX, entranceY, entranceZ);

        this.deepMineManager = new DeepMineManager(this, world, minX, minY, minZ, maxX, maxY, maxZ,
                entrance, resetMinutes);
        deepMineManager.start();
        getServer().getPluginManager().registerEvents(new DeepMineListener(deepMineManager, this), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "shop" -> shopGUI.open(player);
            case "forge" -> forgeGUI.open(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void extractBundledSamples() {
        for (String id : SAMPLE_ITEMS) {
            File file = new File(getDataFolder(), "items/" + id + ".yml");
            if (!file.exists()) {
                saveResource("items/" + id + ".yml", false);
            }
        }
        for (String id : SAMPLE_RUNES) {
            File file = new File(getDataFolder(), "runes/" + id + ".yml");
            if (!file.exists()) {
                saveResource("runes/" + id + ".yml", false);
            }
        }
        for (String id : SAMPLE_ZONES) {
            File file = new File(getDataFolder(), "zones/" + id + ".yml");
            if (!file.exists()) {
                saveResource("zones/" + id + ".yml", false);
            }
        }
        for (String id : SAMPLE_FISHING_HOLES) {
            File file = new File(getDataFolder(), "fishingholes/" + id + ".yml");
            if (!file.exists()) {
                saveResource("fishingholes/" + id + ".yml", false);
            }
        }
        for (String id : SAMPLE_GATES) {
            File file = new File(getDataFolder(), "gates/" + id + ".yml");
            if (!file.exists()) {
                saveResource("gates/" + id + ".yml", false);
            }
        }
    }

    private <T> T lookupService(Class<T> serviceClass) {
        RegisteredServiceProvider<T> provider = Bukkit.getServicesManager().getRegistration(serviceClass);
        return provider == null ? null : provider.getProvider();
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    public GearScoreCalculator getGearScoreCalculator() {
        return gearScoreCalculator;
    }
}
