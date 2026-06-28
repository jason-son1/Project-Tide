package com.tide.core.difficulty;

/** One PI bracket (T1-T5) from difficulty-scaling.brackets in config.yml. */
public record DifficultyBracket(String id, double minPi, double maxPi, double hpScale,
                                 double dmgScale, int maxAffixes, double sellBonus) {

    public boolean contains(double pi) {
        return pi >= minPi && pi <= maxPi;
    }
}
