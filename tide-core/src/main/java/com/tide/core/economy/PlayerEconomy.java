package com.tide.core.economy;

import java.util.UUID;

public final class PlayerEconomy {

    private final UUID uuid;
    private long clam;
    private long pearl;
    private int rep;
    private boolean hardMode;

    public PlayerEconomy(UUID uuid, long clam, long pearl, int rep, boolean hardMode) {
        this.uuid = uuid;
        this.clam = clam;
        this.pearl = pearl;
        this.rep = rep;
        this.hardMode = hardMode;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getClam() {
        return clam;
    }

    public void setClam(long clam) {
        this.clam = Math.max(0, clam);
    }

    public long getPearl() {
        return pearl;
    }

    public void setPearl(long pearl) {
        this.pearl = Math.max(0, pearl);
    }

    public int getRep() {
        return rep;
    }

    public void setRep(int rep) {
        this.rep = Math.max(0, rep);
    }

    public boolean isHardMode() {
        return hardMode;
    }

    public void setHardMode(boolean hardMode) {
        this.hardMode = hardMode;
    }
}
