package com.tide.mobs.quest;

/**
 * Quest trigger type constants.
 * These map to the {@code trigger.type} field in quest YAML files.
 * New types can be added here and wired up in BountyManager without
 * touching any other existing code.
 */
public final class QuestTrigger {

    /** Any successful fishing QTE result (good or perfect). */
    public static final String FISHING_SUCCESS = "FISHING_SUCCESS";

    /** Only a Perfect (≤250 ms) fishing QTE result. */
    public static final String FISHING_PERFECT = "FISHING_PERFECT";

    /** Breaking an ore block inside the Deep Mine. filter = Material name or empty for all. */
    public static final String DEEPMINE_ORE_BREAK = "DEEPMINE_ORE_BREAK";

    /** Participating in a successful boss raid kill. filter = altar ID or empty for any. */
    public static final String BOSS_KILL = "BOSS_KILL";

    /** Killing a custom mob. filter = mob_id or empty for any custom mob. */
    public static final String KILL_MOB = "KILL_MOB";

    /** Killing an elite (starred) mob regardless of affix. */
    public static final String KILL_ELITE = "KILL_ELITE";

    /** Killing an elite mob that has a specific affix. filter = affix id. */
    public static final String KILL_AFFIX = "KILL_AFFIX";

    /** Spending clam currency (shop purchases, etc.). target = amount to spend total. */
    public static final String CLAM_SPEND = "CLAM_SPEND";

    /** Earning reputation (any source). target = amount to earn total. */
    public static final String REP_EARN = "REP_EARN";

    private QuestTrigger() {}
}
