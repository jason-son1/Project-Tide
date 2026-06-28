package com.tide.core.difficulty;

/**
 * Resolved difficulty for one spawn/encounter location: the bracket the
 * effective PI fell into, plus the final HP/damage multipliers (continuous
 * PI curve x bracket step-bonus x TideState multiplier, already combined).
 */
public record DifficultyResult(DifficultyBracket bracket, double effectivePi,
                                double hpMultiplier, double dmgMultiplier) {
}
