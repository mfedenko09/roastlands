package io.github.mfedenko09.roastlands.core.mastery;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class Retention {

    private static final double RETENTION_AT_DUE_DATE = 0.9;

    public double estimate(CardState state, LocalDate today) {
        if (state.lastReviewDate() == null) {
            return 0;
        }
        if (state.intervalDays() <= 0) {
            throw new IllegalArgumentException(
                    "reviewed card must have a positive interval: " + state.cardId());
        }
        long daysSinceReview = Math.max(0, ChronoUnit.DAYS.between(state.lastReviewDate(), today));
        return Math.pow(RETENTION_AT_DUE_DATE, (double) daysSinceReview / state.intervalDays());
    }
}
