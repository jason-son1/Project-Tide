package com.tide.mobs;

import java.io.File;
import com.tide.core.economy.EconomyAPI;
import com.tide.core.reload.ReloadManager;
import com.tide.core.tide.BountyTempoProvider;
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
import com.tide.mobs.boss.AltarCommand;
import com.tide.mobs.nemesis.CalamityManager;
import com.tide.mobs.nemesis.ContractBoardGUI;
import com.tide.mobs.nemesis.ContractBoardListener;
import com.tide.mobs.nemesis.ContractManager;
import com.tide.mobs.nemesis.LegacyTheftManager;
import com.tide.mobs.nemesis.NemesisManager;
import com.tide.mobs.nemesis.NemesisRewardListener;
import com.tide.mobs.nemesis.NemesisTracker;
import com.tide.mobs.nemesis.NemesisTriggerListener;
import com.tide.mobs.quest.BountyBoardGUI;
import com.tide.mobs.quest.BountyBoardListener;
import com.tide.mobs.quest.BountyKillListener;
import com.tide.mobs.quest.BountyManager;
import com.tide.mobs.quest.QuestRegistry;
import com.tide.mobs.mob.MobRegistry;
import com.tide.mobs.mob.CustomMobSpawnListener;
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
            "void_knight_altar", "coral_queen_altar", "abyssal_titan_altar"
    };
    private static final String[] SAMPLE_MOBS = {
            "tide_drowned_corsair", "abyssal_husk", "spring_tide_siren", "bloodmoon_revenant",
            "coral_guardian", "tidal_witch", "rustfang_skeleton", "mine_saboteur_creeper",
            "abyss_phantom", "shrieking_stray", "brine_spider", "pillager_raid_captain"
    };
    private static final String[] SAMPLE_QUESTS = {
            // 낚시 계열
            "fishing_beginner", "fishing_journeyman", "fishing_perfect_touch",
            "fishing_perfect_master", "fishing_weekly_voyage",
            // 딥 마인 계열
            "mine_iron_rush", "mine_gold_fever", "mine_diamond_seeker",
            "mine_deepslate_grind", "mine_emerald_luck", "mine_weekly_depth",
            // 보스 계열
            "boss_first_blood", "boss_weekly_raid",
            // 정예/몹 처치 계열
            "elite_hunter_daily", "elite_hunter_weekly",
            "affix_flame_hunter", "affix_ice_hunter", "affix_explosive_hunter",
            "mob_corsair_hunt", "mob_phantom_hunt", "mob_siren_hunt",
            "mob_witch_hunt", "mob_spider_hunt",
            // 경제/생활 계열
            "clam_spender_daily", "rep_earner_daily", "rep_earner_weekly",
            // 주간 특별
            "weekly_ocean_legend", "weekly_deep_tyrant"
    };

    private AffixRegistry affixRegistry;
    private AltarRegistry altarRegistry;
    private MobRegistry mobRegistry;
    private QuestRegistry questRegistry;
    private BountyManager bountyManager;
    private BountyBoardGUI bountyBoardGUI;
    private NemesisManager nemesisManager;
    private NemesisTracker nemesisTracker;
    private ContractManager contractManager;
    private ContractBoardGUI contractBoardGUI;
    private com.tide.mobs.difficulty.WorldDifficultyRefreshTask worldDifficultyRefreshTask;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();

        TideStateProvider tideStateProvider = lookupService(TideStateProvider.class);
        EconomyAPI economyAPI = lookupService(EconomyAPI.class);
        if (tideStateProvider == null || economyAPI == null) {
            getLogger().severe("TideCore의 서비스(TideStateProvider/EconomyAPI)를 찾을 수 없습니다. TideCore가 먼저 로드되어야 합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ItemFactory itemFactory = lookupService(ItemFactory.class);
        com.tide.core.difficulty.DifficultyManager difficultyManager = lookupService(com.tide.core.difficulty.DifficultyManager.class);

        extractBundled();

        this.affixRegistry = new AffixRegistry(this);
        this.altarRegistry = new AltarRegistry(this);
        this.mobRegistry = new MobRegistry(this);
        this.questRegistry = new QuestRegistry(this);
        affixRegistry.reload();
        altarRegistry.reload();
        mobRegistry.reload();
        questRegistry.reload();

        EliteProcessor eliteProcessor = new EliteProcessor();
        getServer().getPluginManager().registerEvents(
                new EliteSpawnListener(this, tideStateProvider, affixRegistry, eliteProcessor, difficultyManager), this);
        getServer().getPluginManager().registerEvents(
                new CustomMobSpawnListener(tideStateProvider, mobRegistry, affixRegistry, eliteProcessor, itemFactory, economyAPI, difficultyManager), this);
        getServer().getPluginManager().registerEvents(new AffixCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new EliteDropListener(itemFactory, economyAPI, difficultyManager), this);

        // Keeps already-spawned mobs' HP/damage in sync with the world's current
        // difficulty (player progression, tide state) instead of freezing them at
        // whatever the world looked like the instant they spawned.
        this.worldDifficultyRefreshTask = new com.tide.mobs.difficulty.WorldDifficultyRefreshTask(this, difficultyManager);
        worldDifficultyRefreshTask.start();

        BossFightManager bossFightManager = new BossFightManager(this, economyAPI);
        bossFightManager.setItemFactory(itemFactory);
        bossFightManager.start();
        getServer().getPluginManager().registerEvents(new AltarInteractListener(altarRegistry, bossFightManager), this);
        getServer().getPluginManager().registerEvents(new BossCombatListener(bossFightManager), this);
        getServer().getPluginManager().registerEvents(new com.tide.mobs.boss.AltarWorldGenListener(this, altarRegistry), this);

        this.bountyManager = new BountyManager(questRegistry, economyAPI);
        if (getConfig().contains("bounty.daily-reset-interval-minutes")) {
            bountyManager.setDailyResetIntervalMinutes(getConfig().getLong("bounty.daily-reset-interval-minutes"));
        }
        if (getConfig().contains("bounty.weekly-reset-interval-minutes")) {
            bountyManager.setWeeklyResetIntervalMinutes(getConfig().getLong("bounty.weekly-reset-interval-minutes"));
        }
        this.bountyBoardGUI = new BountyBoardGUI(bountyManager);
        getServer().getPluginManager().registerEvents(new BountyKillListener(bountyManager), this);
        getServer().getPluginManager().registerEvents(new BountyBoardListener(bountyManager, bountyBoardGUI), this);

        this.nemesisManager = new NemesisManager(this);
        nemesisManager.init();
        CalamityManager calamityManager = new CalamityManager(affixRegistry);
        LegacyTheftManager legacyTheftManager = new LegacyTheftManager();
        this.contractManager = new ContractManager(nemesisManager, economyAPI);
        this.contractBoardGUI = new ContractBoardGUI(contractManager);
        getServer().getPluginManager().registerEvents(
                new NemesisTriggerListener(this, nemesisManager, calamityManager, legacyTheftManager), this);
        getServer().getPluginManager().registerEvents(
                new NemesisRewardListener(nemesisManager, economyAPI, itemFactory, legacyTheftManager, contractManager), this);
        getServer().getPluginManager().registerEvents(new ContractBoardListener(contractManager), this);
        this.nemesisTracker = new NemesisTracker(this, nemesisManager, calamityManager);
        nemesisTracker.start();

        Bukkit.getServicesManager().register(NemesisManager.class, nemesisManager, this, org.bukkit.plugin.ServicePriority.Normal);
        Bukkit.getServicesManager().register(BountyManager.class, bountyManager, this, org.bukkit.plugin.ServicePriority.Normal);
        Bukkit.getServicesManager().register(BountyTempoProvider.class, bountyManager, this, org.bukkit.plugin.ServicePriority.Normal);

        ReloadManager reloadManager = lookupService(ReloadManager.class);
        if (reloadManager != null) {
            reloadManager.register("affixes", affixRegistry);
            reloadManager.register("altars", altarRegistry);
            reloadManager.register("mobs", mobRegistry);
            reloadManager.register("quests", questRegistry);
        }

        getCommand("bounty").setExecutor(this);
        getCommand("bountycontract").setExecutor(this);
        AltarCommand altarCommand = new AltarCommand(this, altarRegistry);
        getCommand("altar").setExecutor(altarCommand);
        getCommand("altar").setTabCompleter(altarCommand);

        getLogger().info("TideMobs enabled. 접두사 " + affixRegistry.all().size() + "개 로드.");
    }

    @Override
    public void onDisable() {
        if (worldDifficultyRefreshTask != null) {
            worldDifficultyRefreshTask.stop();
        }
        if (nemesisTracker != null) {
            nemesisTracker.stop();
        }
        if (nemesisManager != null) {
            nemesisManager.shutdown();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("bounty")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("tempo")) {
                    return handleBountyTempo(player, args);
                }
                if (args[0].equalsIgnoreCase("reset")) {
                    return handleBountyReset(player, args);
                }
            }
            bountyBoardGUI.open(player);
            return true;
        }
        if (command.getName().equalsIgnoreCase("bountycontract")) {
            return handleContractCommand(player, args);
        }
        return false;
    }

    private boolean handleBountyTempo(Player player, String[] args) {
        if (!player.hasPermission("tide.admin")) {
            player.sendMessage("§c권한이 없습니다.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("§c사용법: /bounty tempo <일일_퀘스트_주기_분> <주간_퀘스트_주기_분>");
            return true;
        }
        try {
            long daily = Long.parseLong(args[1]);
            long weekly = Long.parseLong(args[2]);
            if (daily < 1 || weekly < 1) {
                player.sendMessage("§c주기는 최소 1분 이상이어야 합니다.");
                return true;
            }
            bountyManager.setDailyResetIntervalMinutes(daily);
            bountyManager.setWeeklyResetIntervalMinutes(weekly);
            getConfig().set("bounty.daily-reset-interval-minutes", daily);
            getConfig().set("bounty.weekly-reset-interval-minutes", weekly);
            saveConfig();
            player.sendMessage("§a현상금 퀘스트 재생 주기를 일일: §f" + daily + "분§a, 주간: §f" + weekly + "분§a으로 조정했습니다.");
        } catch (NumberFormatException exception) {
            player.sendMessage("§c올바른 숫자를 입력해주세요.");
        }
        return true;
    }

    private boolean handleBountyReset(Player player, String[] args) {
        if (!player.hasPermission("tide.admin")) {
            player.sendMessage("§c권한이 없습니다.");
            return true;
        }
        Player target = player;
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[1]);
                return true;
            }
        }
        bountyManager.forceReset(target);
        player.sendMessage("§a" + target.getName() + " 플레이어의 현상금 퀘스트를 강제 초기화 및 재생성했습니다.");
        return true;
    }

    private boolean handleContractCommand(Player player, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            contractBoardGUI.open(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("post")) {
            if (args.length < 3) {
                player.sendMessage("§c사용법: /bountycontract post <선불금_조개> <성공보수_진주>");
                return true;
            }
            try {
                long upfront = Long.parseLong(args[1]);
                long reward = Long.parseLong(args[2]);
                contractManager.post(player, upfront, reward);
            } catch (NumberFormatException exception) {
                player.sendMessage("§c선불금/성공보수는 숫자여야 합니다.");
            }
            return true;
        }
        player.sendMessage("§c사용법: /bountycontract <post <선불금> <성공보수>|list>");
        return true;
    }

    private void extractBundled() {
        for (String id : SAMPLE_AFFIXES) {
            File file = new File(getDataFolder(), "affixes/" + id + ".yml");
            if (!file.exists()) {
                saveResource("affixes/" + id + ".yml", false);
            }
        }
        for (String id : SAMPLE_ALTARS) {
            File file = new File(getDataFolder(), "altars/" + id + ".yml");
            if (!file.exists()) {
                saveResource("altars/" + id + ".yml", false);
            }
        }
        for (String id : SAMPLE_MOBS) {
            File file = new File(getDataFolder(), "mobs/" + id + ".yml");
            if (!file.exists()) {
                saveResource("mobs/" + id + ".yml", false);
            }
        }
        for (String id : SAMPLE_QUESTS) {
            File file = new File(getDataFolder(), "quests/" + id + ".yml");
            if (!file.exists()) {
                saveResource("quests/" + id + ".yml", false);
            }
        }
    }

    private <T> T lookupService(Class<T> serviceClass) {
        RegisteredServiceProvider<T> provider = Bukkit.getServicesManager().getRegistration(serviceClass);
        return provider == null ? null : provider.getProvider();
    }
}
