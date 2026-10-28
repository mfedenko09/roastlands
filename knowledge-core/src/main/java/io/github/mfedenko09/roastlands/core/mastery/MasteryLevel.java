package io.github.mfedenko09.roastlands.core.mastery;

public enum MasteryLevel {
    NEW,
    RUSTY,
    WARM,
    SHARP;

    private static final double WARM_THRESHOLD = 0.4;
    private static final double SHARP_THRESHOLD = 0.75;

    public static MasteryLevel of(double mastery, boolean anyReviewed) {
        if (!anyReviewed) {
            return NEW;
        }
        if (mastery < WARM_THRESHOLD) {
            return RUSTY;
        }
        return mastery < SHARP_THRESHOLD ? WARM : SHARP;
    }
}
