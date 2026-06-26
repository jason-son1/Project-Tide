package com.tide.mobs.nemesis;

import java.util.UUID;

/** Bounty Brokerage & Contracts (3-3): a paid request to hunt someone else's nemesis. */
public final class ContractRecord {

    private final UUID contractId;
    private final UUID posterUuid;
    private final UUID nemesisMobUuid;
    private final String nemesisName;
    private final long upfrontClam;
    private final long rewardPearl;
    private UUID mercenaryUuid;

    public ContractRecord(UUID contractId, UUID posterUuid, UUID nemesisMobUuid, String nemesisName,
                           long upfrontClam, long rewardPearl) {
        this.contractId = contractId;
        this.posterUuid = posterUuid;
        this.nemesisMobUuid = nemesisMobUuid;
        this.nemesisName = nemesisName;
        this.upfrontClam = upfrontClam;
        this.rewardPearl = rewardPearl;
    }

    public UUID getContractId() {
        return contractId;
    }

    public UUID getPosterUuid() {
        return posterUuid;
    }

    public UUID getNemesisMobUuid() {
        return nemesisMobUuid;
    }

    public String getNemesisName() {
        return nemesisName;
    }

    public long getUpfrontClam() {
        return upfrontClam;
    }

    public long getRewardPearl() {
        return rewardPearl;
    }

    public UUID getMercenaryUuid() {
        return mercenaryUuid;
    }

    public boolean isAccepted() {
        return mercenaryUuid != null;
    }

    public void accept(UUID mercenaryUuid) {
        this.mercenaryUuid = mercenaryUuid;
    }
}
