package io.github.mfedenko09.roastlands.core.srs;

import java.time.LocalDate;

public final class Sm2Scheduler {

    private static final double MIN_EASE_FACTOR = 1.3;
    private static final int FIRST_INTERVAL_DAYS = 1;
    private static final int SECOND_INTERVAL_DAYS = 6;
    private static final int PASSING_QUALITY = 3;
    private static final int PERFECT_QUALITY = 5;
    private static final double EASE_BASE_STEP = 0.1;
    private static final double EASE_LINEAR_PENALTY = 0.08;
    private static final double EASE_QUADRATIC_PENALTY = 0.02;

    public CardState review(CardState state, Grade grade, LocalDate today) {
        int quality = grade.quality();
        boolean failed = quality < PASSING_QUALITY;
        int intervalDays = failed ? FIRST_INTERVAL_DAYS : nextInterval(state);
        return state.toBuilder()
                .repetitions(failed ? 0 : state.repetitions() + 1)
                .easeFactor(nextEaseFactor(state.easeFactor(), quality))
                .intervalDays(intervalDays)
                .dueDate(today.plusDays(intervalDays))
                .firstSeenDate(firstSeenDate(state, today))
                .lastReviewDate(today)
                .lapses(failed ? state.lapses() + 1 : state.lapses())
                .build();
    }

    private LocalDate firstSeenDate(CardState state, LocalDate today) {
        if (state.firstSeenDate() != null) {
            return state.firstSeenDate();
        }
        return state.lastReviewDate() == null ? today : null;
    }

    private int nextInterval(CardState state) {
        return switch (state.repetitions()) {
            case 0 -> FIRST_INTERVAL_DAYS;
            case 1 -> SECOND_INTERVAL_DAYS;
            default -> (int) Math.round(state.intervalDays() * state.easeFactor());
        };
    }

    private double nextEaseFactor(double easeFactor, int quality) {
        int distance = PERFECT_QUALITY - quality;
        double adjusted = easeFactor + (EASE_BASE_STEP
                - distance * (EASE_LINEAR_PENALTY + distance * EASE_QUADRATIC_PENALTY));
        return Math.max(MIN_EASE_FACTOR, adjusted);
    }
}
