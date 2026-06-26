package com.tide.mobs.quest;

public final class BountyQuest {

    private final BountyType type;
    private final String affixFilter;
    private final int targetCount;
    private final long rewardClam;
    private final int rewardRep;
    private final boolean weekly;
    private int progress;
    private boolean claimed;

    public BountyQuest(BountyType type, String affixFilter, int targetCount,
                        long rewardClam, int rewardRep, boolean weekly) {
        this.type = type;
        this.affixFilter = affixFilter;
        this.targetCount = targetCount;
        this.rewardClam = rewardClam;
        this.rewardRep = rewardRep;
        this.weekly = weekly;
    }

    public boolean matches(boolean isElite, String affixesCsv) {
        if (type == BountyType.KILL_ELITE) {
            return isElite;
        }
        return isElite && affixesCsv != null && affixFilter != null && affixesCsv.contains(affixFilter);
    }

    public void incrementProgress() {
        if (!isComplete()) {
            progress++;
        }
    }

    public boolean isComplete() {
        return progress >= targetCount;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void markClaimed() {
        claimed = true;
    }

    public boolean isWeekly() {
        return weekly;
    }

    public int getProgress() {
        return progress;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public long getRewardClam() {
        return rewardClam;
    }

    public int getRewardRep() {
        return rewardRep;
    }

    public String describe() {
        String label = type == BountyType.KILL_ELITE
                ? "정예 몹 처치"
                : "[" + affixFilter + "] 접두사 몹 처치";
        return (weekly ? "§6[주간] " : "§f[일일] ") + label;
    }
}
