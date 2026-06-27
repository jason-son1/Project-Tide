package com.tide.mobs.quest;

import org.bukkit.Material;

/**
 * A single active quest instance assigned to a player.
 *
 * <p>Each instance is created by {@link QuestTemplate#instantiateDaily()} or
 * {@link QuestTemplate#instantiateWeekly()} with randomised target/reward
 * values drawn from the template's configured ranges.
 */
public final class BountyQuest {

    private final QuestTemplate template;
    private final int  targetCount;
    private final long rewardClam;
    private final int  rewardRep;
    private final boolean weekly;

    private int  progress;
    private boolean claimed;

    BountyQuest(QuestTemplate template, int targetCount, long rewardClam, int rewardRep, boolean weekly) {
        this.template    = template;
        this.targetCount = Math.max(1, targetCount);
        this.rewardClam  = rewardClam;
        this.rewardRep   = rewardRep;
        this.weekly      = weekly;
    }

    // ── Trigger matching ──────────────────────────────────────────────────────

    /**
     * Returns true if this quest should count the given event.
     *
     * @param triggerType  one of the {@link QuestTrigger} constants
     * @param eventDetail  optional detail (mob id, affix id, material name, …)
     */
    public boolean matchesTrigger(String triggerType, String eventDetail) {
        if (!template.getTriggerType().equalsIgnoreCase(triggerType)) return false;
        String filter = template.getTriggerFilter();
        if (filter == null || filter.isBlank()) return true;  // no filter → match all
        return eventDetail != null && eventDetail.toUpperCase().contains(filter.toUpperCase());
    }

    // ── Progress ──────────────────────────────────────────────────────────────

    public void incrementProgress() {
        if (!isComplete()) progress++;
    }

    public boolean isComplete() { return progress >= targetCount; }
    public boolean isClaimed()  { return claimed; }
    public void markClaimed()   { claimed = true; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public QuestTemplate getTemplate()  { return template; }
    public int  getProgress()           { return progress; }
    public int  getTargetCount()        { return targetCount; }
    public long getRewardClam()         { return rewardClam; }
    public int  getRewardRep()          { return rewardRep; }
    public boolean isWeekly()           { return weekly; }

    /** Human-readable title for chat / GUI. */
    public String describe() {
        String prefix = weekly ? "§6[주간] " : "§f[일일] ";
        return prefix + template.getName();
    }

    /** GUI icon material. */
    public Material getIcon() { return template.getIcon(); }

    /** Short description line for GUI lore. */
    public String getDescription() { return template.getDescription(); }
}
