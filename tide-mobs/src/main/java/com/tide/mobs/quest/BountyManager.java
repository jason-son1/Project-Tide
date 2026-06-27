package com.tide.mobs.quest;

import com.tide.core.economy.EconomyAPI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory daily/weekly bounty state per player.
 *
 * <p>Quest pools are fully driven by {@link QuestRegistry} — no hardcoded
 * quest types remain here. A hot-reload of the registry (via /tide reload)
 * will cause new quests to appear for players on their next refresh cycle.
 */
public final class BountyManager implements com.tide.core.tide.BountyTempoProvider {

    private final QuestRegistry questRegistry;
    private final EconomyAPI economyAPI;

    private final Map<UUID, List<BountyQuest>> dailyQuests  = new ConcurrentHashMap<>();
    private final Map<UUID, BountyQuest>       weeklyQuests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDailyResetTime  = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastWeeklyResetTime = new ConcurrentHashMap<>();

    private long dailyResetIntervalMinutes  = 1440;
    private long weeklyResetIntervalMinutes = 10080;

    public BountyManager(QuestRegistry questRegistry, EconomyAPI economyAPI) {
        this.questRegistry = questRegistry;
        this.economyAPI    = economyAPI;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public List<BountyQuest> getQuests(Player player) {
        ensureFresh(player);
        List<BountyQuest> all = new ArrayList<>(dailyQuests.getOrDefault(player.getUniqueId(), List.of()));
        BountyQuest weekly = weeklyQuests.get(player.getUniqueId());
        if (weekly != null) all.add(weekly);
        return all;
    }

    public void forceReset(Player player) {
        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();
        dailyQuests.put(uuid, generateDaily());
        weeklyQuests.put(uuid, generateWeekly());
        lastDailyResetTime.put(uuid, now);
        lastWeeklyResetTime.put(uuid, now);
    }

    public boolean claim(Player player, BountyQuest quest) {
        if (!quest.isComplete() || quest.isClaimed()) return false;
        quest.markClaimed();
        economyAPI.addClam(player.getUniqueId(), quest.getRewardClam());
        economyAPI.addRep(player.getUniqueId(), quest.getRewardRep());
        return true;
    }

    // ── Trigger entry points (called from listeners / cross-module reflection) ─

    /**
     * Call when a player successfully reels in a fish.
     * @param isPerfect true if the QTE was graded Perfect (≤250 ms)
     */
    public void onFishing(Player player, boolean isPerfect) {
        for (BountyQuest q : getQuests(player)) {
            if (q.isComplete()) continue;
            if (q.matchesTrigger(QuestTrigger.FISHING_SUCCESS, null)) {
                q.incrementProgress();
                notifyIfComplete(player, q);
            }
            // FISHING_PERFECT quests also count on perfect catches
            if (isPerfect && q.matchesTrigger(QuestTrigger.FISHING_PERFECT, null)) {
                q.incrementProgress();
                notifyIfComplete(player, q);
            }
        }
    }

    /**
     * Call when a player breaks an ore block inside the Deep Mine.
     * @param blockName Material.name() of the broken block
     */
    public void onMining(Player player, String blockName) {
        for (BountyQuest q : getQuests(player)) {
            if (q.isComplete()) continue;
            if (q.matchesTrigger(QuestTrigger.DEEPMINE_ORE_BREAK, blockName)) {
                q.incrementProgress();
                notifyIfComplete(player, q);
            }
        }
    }

    /**
     * Call when a player participates in a successful boss kill.
     * @param bossId altar/boss id, or empty string if unknown
     */
    public void onBossKill(Player player, String bossId) {
        for (BountyQuest q : getQuests(player)) {
            if (q.isComplete()) continue;
            if (q.matchesTrigger(QuestTrigger.BOSS_KILL, bossId)) {
                q.incrementProgress();
                notifyIfComplete(player, q);
            }
        }
    }

    /** Backwards-compat overload — boss id unknown. */
    public void onBossKill(Player player) {
        onBossKill(player, "");
    }

    /**
     * Call when a player kills an elite (or affix) mob.
     * @param isElite     true if the mob was elite
     * @param affixesCsv  comma-separated affix ids, or null
     * @param mobId       custom mob id (e.g. tide_drowned_corsair), or null
     */
    public void onEliteKill(Player player, boolean isElite, String affixesCsv, String mobId) {
        for (BountyQuest q : getQuests(player)) {
            if (q.isComplete()) continue;
            // KILL_MOB — any custom mob (filter = specific mob id or empty)
            if (q.matchesTrigger(QuestTrigger.KILL_MOB, mobId)) {
                q.incrementProgress();
                notifyIfComplete(player, q);
                continue;
            }
            if (!isElite) continue;
            // KILL_ELITE — any elite mob
            if (q.matchesTrigger(QuestTrigger.KILL_ELITE, null)) {
                q.incrementProgress();
                notifyIfComplete(player, q);
            }
            // KILL_AFFIX — elite with specific affix
            if (affixesCsv != null && q.matchesTrigger(QuestTrigger.KILL_AFFIX, affixesCsv)) {
                q.incrementProgress();
                notifyIfComplete(player, q);
            }
        }
    }

    /** Backwards-compat overload used by BountyKillListener (no mob id). */
    public void onEliteKill(Player player, boolean isElite, String affixesCsv) {
        onEliteKill(player, isElite, affixesCsv, null);
    }

    /**
     * Call when a player spends clam (e.g. shop purchase).
     * @param amount amount spent
     */
    public void onClamSpend(Player player, long amount) {
        for (BountyQuest q : getQuests(player)) {
            if (q.isComplete()) continue;
            if (q.matchesTrigger(QuestTrigger.CLAM_SPEND, null)) {
                // Each call = 1 progress unit; callers should call once per transaction
                q.incrementProgress();
                notifyIfComplete(player, q);
            }
        }
    }

    /**
     * Call when a player earns reputation.
     * @param amount reputation earned
     */
    public void onRepEarn(Player player, int amount) {
        for (BountyQuest q : getQuests(player)) {
            if (q.isComplete()) continue;
            if (q.matchesTrigger(QuestTrigger.REP_EARN, null)) {
                q.incrementProgress();
                notifyIfComplete(player, q);
            }
        }
    }

    // ── Reset interval config ────────────────────────────────────────────────

    public long getDailyResetIntervalMinutes()  { return dailyResetIntervalMinutes; }
    public long getWeeklyResetIntervalMinutes() { return weeklyResetIntervalMinutes; }

    public void setDailyResetIntervalMinutes(long minutes) {
        this.dailyResetIntervalMinutes = Math.max(1, minutes);
        persistConfig("bounty.daily-reset-interval-minutes", dailyResetIntervalMinutes);
    }

    public void setWeeklyResetIntervalMinutes(long minutes) {
        this.weeklyResetIntervalMinutes = Math.max(1, minutes);
        persistConfig("bounty.weekly-reset-interval-minutes", weeklyResetIntervalMinutes);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void ensureFresh(Player player) {
        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        Long lastDaily = lastDailyResetTime.get(uuid);
        if (lastDaily == null || (now - lastDaily) >= dailyResetIntervalMinutes * 60_000L) {
            dailyQuests.put(uuid, generateDaily());
            lastDailyResetTime.put(uuid, now);
        }

        Long lastWeekly = lastWeeklyResetTime.get(uuid);
        if (lastWeekly == null || (now - lastWeekly) >= weeklyResetIntervalMinutes * 60_000L) {
            BountyQuest wq = generateWeekly();
            if (wq != null) weeklyQuests.put(uuid, wq);
            lastWeeklyResetTime.put(uuid, now);
        }
    }

    private List<BountyQuest> generateDaily() {
        List<BountyQuest> quests = new ArrayList<>();
        for (QuestTemplate t : questRegistry.drawDaily(3)) {
            quests.add(t.instantiateDaily());
        }
        return quests;
    }

    private BountyQuest generateWeekly() {
        QuestTemplate t = questRegistry.drawWeekly();
        return t == null ? null : t.instantiateWeekly();
    }

    private void notifyIfComplete(Player player, BountyQuest quest) {
        if (quest.isComplete()) {
            player.sendMessage("§a현상금 목표 달성! §f" + quest.describe() + " §7- /bounty 에서 보상을 수령하세요.");
        }
    }

    private void persistConfig(String key, long value) {
        try {
            com.tide.mobs.TideMobsPlugin plugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.tide.mobs.TideMobsPlugin.class);
            plugin.getConfig().set(key, value);
            plugin.saveConfig();
        } catch (Exception ignored) {}
    }
}
