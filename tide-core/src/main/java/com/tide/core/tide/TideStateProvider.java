package com.tide.core.tide;

/**
 * Cross-jar contract so TideRPG/TideMobs can read the current tide state
 * via ServicesManager without depending on TideCore's TideScheduler class directly.
 */
public interface TideStateProvider {

    TideState getCurrentState();

    long getSecondsUntilNextChange();
}
