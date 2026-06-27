package com.tide.mobs.quest;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Immutable definition of a quest loaded from a YAML config file.
 *
 * <p>YAML structure (quests/&lt;id&gt;.yml):
 * <pre>
 * id: fishing_beginner
 * name: "§e초보 낚시꾼"
 * description: "바다의 맛을 느껴보자!"
 * icon: FISHING_ROD
 * schedule: daily          # daily | weekly | both
 * weight: 10               # relative draw weight
 *
 * trigger:
 *   type: FISHING_SUCCESS  # see QuestTrigger constants
 *   filter: ""             # optional; affix id / mob id / material name
 *
 * target:
 *   daily_min: 5
 *   daily_max: 10
 *   weekly_min: 20
 *   weekly_max: 30
 *
 * reward:
 *   clam:
 *     daily_min: 80
 *     daily_max: 200
 *     weekly_min: 600
 *     weekly_max: 900
 *   rep:
 *     daily_min: 4
 *     daily_max: 12
 *     weekly_min: 35
 *     weekly_max: 50
 * </pre>
 */
public final class QuestTemplate {

    public static final String SCHEDULE_DAILY  = "daily";
    public static final String SCHEDULE_WEEKLY = "weekly";
    public static final String SCHEDULE_BOTH   = "both";

    private final String id;
    private final String name;
    private final String description;
    private final Material icon;
    private final String schedule;
    private final int weight;

    private final String triggerType;
    private final String triggerFilter;

    private final int targetDailyMin;
    private final int targetDailyMax;
    private final int targetWeeklyMin;
    private final int targetWeeklyMax;

    private final long rewardClamDailyMin;
    private final long rewardClamDailyMax;
    private final long rewardClamWeeklyMin;
    private final long rewardClamWeeklyMax;

    private final int rewardRepDailyMin;
    private final int rewardRepDailyMax;
    private final int rewardRepWeeklyMin;
    private final int rewardRepWeeklyMax;

    private QuestTemplate(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.description = b.description;
        this.icon = b.icon;
        this.schedule = b.schedule;
        this.weight = b.weight;
        this.triggerType = b.triggerType;
        this.triggerFilter = b.triggerFilter;
        this.targetDailyMin = b.targetDailyMin;
        this.targetDailyMax = b.targetDailyMax;
        this.targetWeeklyMin = b.targetWeeklyMin;
        this.targetWeeklyMax = b.targetWeeklyMax;
        this.rewardClamDailyMin = b.rewardClamDailyMin;
        this.rewardClamDailyMax = b.rewardClamDailyMax;
        this.rewardClamWeeklyMin = b.rewardClamWeeklyMin;
        this.rewardClamWeeklyMax = b.rewardClamWeeklyMax;
        this.rewardRepDailyMin = b.rewardRepDailyMin;
        this.rewardRepDailyMax = b.rewardRepDailyMax;
        this.rewardRepWeeklyMin = b.rewardRepWeeklyMin;
        this.rewardRepWeeklyMax = b.rewardRepWeeklyMax;
    }

    /** Load a QuestTemplate from a YAML file. Returns null if invalid. */
    public static QuestTemplate fromFile(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        try {
            Builder b = new Builder();
            b.id            = cfg.getString("id", file.getName().replace(".yml", ""));
            b.name          = cfg.getString("name", "§f" + b.id);
            b.description   = cfg.getString("description", "");
            b.schedule      = cfg.getString("schedule", SCHEDULE_DAILY).toLowerCase();
            b.weight        = cfg.getInt("weight", 10);

            String iconStr  = cfg.getString("icon", "PAPER");
            b.icon = parseMaterial(iconStr, Material.PAPER);

            b.triggerType   = cfg.getString("trigger.type", "").toUpperCase();
            b.triggerFilter = cfg.getString("trigger.filter", "");

            b.targetDailyMin  = cfg.getInt("target.daily_min", 1);
            b.targetDailyMax  = cfg.getInt("target.daily_max", b.targetDailyMin);
            b.targetWeeklyMin = cfg.getInt("target.weekly_min", b.targetDailyMin * 3);
            b.targetWeeklyMax = cfg.getInt("target.weekly_max", b.targetDailyMax * 4);

            b.rewardClamDailyMin  = cfg.getLong("reward.clam.daily_min", 50);
            b.rewardClamDailyMax  = cfg.getLong("reward.clam.daily_max", b.rewardClamDailyMin);
            b.rewardClamWeeklyMin = cfg.getLong("reward.clam.weekly_min", b.rewardClamDailyMin * 5);
            b.rewardClamWeeklyMax = cfg.getLong("reward.clam.weekly_max", b.rewardClamDailyMax * 6);

            b.rewardRepDailyMin  = cfg.getInt("reward.rep.daily_min", 5);
            b.rewardRepDailyMax  = cfg.getInt("reward.rep.daily_max", b.rewardRepDailyMin);
            b.rewardRepWeeklyMin = cfg.getInt("reward.rep.weekly_min", b.rewardRepDailyMin * 4);
            b.rewardRepWeeklyMax = cfg.getInt("reward.rep.weekly_max", b.rewardRepDailyMax * 5);

            if (b.triggerType.isEmpty()) return null;
            return new QuestTemplate(b);
        } catch (Exception e) {
            return null;
        }
    }

    private static Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    // ── Instantiation helpers ─────────────────────────────────────────────────

    /** Create a BountyQuest instance from this template for a daily slot. */
    public BountyQuest instantiateDaily() {
        int target   = randInt(targetDailyMin, targetDailyMax);
        long clam    = randLong(rewardClamDailyMin, rewardClamDailyMax);
        int rep      = randInt(rewardRepDailyMin, rewardRepDailyMax);
        return new BountyQuest(this, target, clam, rep, false);
    }

    /** Create a BountyQuest instance from this template for a weekly slot. */
    public BountyQuest instantiateWeekly() {
        int target   = randInt(targetWeeklyMin, targetWeeklyMax);
        long clam    = randLong(rewardClamWeeklyMin, rewardClamWeeklyMax);
        int rep      = randInt(rewardRepWeeklyMin, rewardRepWeeklyMax);
        return new BountyQuest(this, target, clam, rep, true);
    }

    private static int randInt(int min, int max) {
        return min >= max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static long randLong(long min, long max) {
        return min >= max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()            { return id; }
    public String getName()          { return name; }
    public String getDescription()   { return description; }
    public Material getIcon()        { return icon; }
    public String getSchedule()      { return schedule; }
    public int getWeight()           { return weight; }
    public String getTriggerType()   { return triggerType; }
    public String getTriggerFilter() { return triggerFilter; }

    public boolean isAvailableForDaily()  {
        return SCHEDULE_DAILY.equals(schedule) || SCHEDULE_BOTH.equals(schedule);
    }
    public boolean isAvailableForWeekly() {
        return SCHEDULE_WEEKLY.equals(schedule) || SCHEDULE_BOTH.equals(schedule);
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private static final class Builder {
        String id, name, description, schedule, triggerType, triggerFilter;
        Material icon;
        int weight;
        int targetDailyMin, targetDailyMax, targetWeeklyMin, targetWeeklyMax;
        long rewardClamDailyMin, rewardClamDailyMax, rewardClamWeeklyMin, rewardClamWeeklyMax;
        int rewardRepDailyMin, rewardRepDailyMax, rewardRepWeeklyMin, rewardRepWeeklyMax;
    }
}
