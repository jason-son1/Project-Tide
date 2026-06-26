package com.tide.mobs.quest;

import com.tide.core.economy.EconomyAPI;
import com.tide.mobs.affix.AffixDefinition;
import com.tide.mobs.affix.AffixRegistry;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * In-memory daily/weekly bounty state per player — regenerated when the
 * calendar date (daily) or ISO week (weekly) rolls over. Lost on restart;
 * acceptable for a prototype, persistence can be added later if needed.
 */
public final class BountyManager {

    private final AffixRegistry affixRegistry;
    private final EconomyAPI economyAPI;
    private final Map<UUID, List<BountyQuest>> dailyQuests = new ConcurrentHashMap<>();
    private final Map<UUID, BountyQuest> weeklyQuests = new ConcurrentHashMap<>();
    private final Map<UUID, LocalDate> lastDailyReset = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastWeeklyReset = new ConcurrentHashMap<>();

    public BountyManager(AffixRegistry affixRegistry, EconomyAPI economyAPI) {
        this.affixRegistry = affixRegistry;
        this.economyAPI = economyAPI;
    }

    public List<BountyQuest> getQuests(Player player) {
        ensureFresh(player);
        List<BountyQuest> all = new ArrayList<>(dailyQuests.getOrDefault(player.getUniqueId(), List.of()));
        BountyQuest weekly = weeklyQuests.get(player.getUniqueId());
        if (weekly != null) {
            all.add(weekly);
        }
        return all;
    }

    private void ensureFresh(Player player) {
        UUID uuid = player.getUniqueId();
        LocalDate today = LocalDate.now();
        if (!today.equals(lastDailyReset.get(uuid))) {
            dailyQuests.put(uuid, generateDaily());
            lastDailyReset.put(uuid, today);
        }
        int isoWeek = today.get(IsoFields.WEEK_BASED_YEAR) * 100 + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        if (!Integer.valueOf(isoWeek).equals(lastWeeklyReset.get(uuid))) {
            weeklyQuests.put(uuid, generateWeekly());
            lastWeeklyReset.put(uuid, isoWeek);
        }
    }

    private List<BountyQuest> generateDaily() {
        List<BountyQuest> quests = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            quests.add(randomQuest(false));
        }
        return quests;
    }

    private BountyQuest generateWeekly() {
        BountyQuest quest = randomQuest(true);
        return quest;
    }

    private BountyQuest randomQuest(boolean weekly) {
        boolean useAffix = !affixRegistry.all().isEmpty() && ThreadLocalRandom.current().nextBoolean();
        int baseTarget = weekly ? ThreadLocalRandom.current().nextInt(15, 26) : ThreadLocalRandom.current().nextInt(3, 8);
        long rewardClam = weekly ? 800 : ThreadLocalRandom.current().nextInt(100, 251);
        int rewardRep = weekly ? 50 : ThreadLocalRandom.current().nextInt(5, 16);

        if (useAffix) {
            List<AffixDefinition> pool = affixRegistry.all();
            AffixDefinition affix = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            return new BountyQuest(BountyType.KILL_AFFIX, affix.getId(), baseTarget, rewardClam, rewardRep, weekly);
        }
        return new BountyQuest(BountyType.KILL_ELITE, null, baseTarget, rewardClam, rewardRep, weekly);
    }

    public void onEliteKill(Player player, boolean isElite, String affixesCsv) {
        for (BountyQuest quest : getQuests(player)) {
            if (!quest.isComplete() && quest.matches(isElite, affixesCsv)) {
                quest.incrementProgress();
                if (quest.isComplete()) {
                    player.sendMessage("§a현상금 목표 달성! §f" + quest.describe() + " §7- /bounty 에서 보상을 수령하세요.");
                }
            }
        }
    }

    public boolean claim(Player player, BountyQuest quest) {
        if (!quest.isComplete() || quest.isClaimed()) {
            return false;
        }
        quest.markClaimed();
        economyAPI.addClam(player.getUniqueId(), quest.getRewardClam());
        economyAPI.addRep(player.getUniqueId(), quest.getRewardRep());
        return true;
    }
}
