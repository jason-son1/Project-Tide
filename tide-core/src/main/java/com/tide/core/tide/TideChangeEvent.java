package com.tide.core.tide;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired whenever TideScheduler transitions state. Other jars listen to this
 * instead of polling, so each part only reacts within its own responsibility
 * (TideRPG adjusts drop rates, TideMobs adjusts spawn rates, etc.) with no
 * direct cross-jar method calls.
 */
public final class TideChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TideState previousState;
    private final TideState newState;

    public TideChangeEvent(TideState previousState, TideState newState) {
        super(false);
        this.previousState = previousState;
        this.newState = newState;
    }

    public TideState getPreviousState() {
        return previousState;
    }

    public TideState getNewState() {
        return newState;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
