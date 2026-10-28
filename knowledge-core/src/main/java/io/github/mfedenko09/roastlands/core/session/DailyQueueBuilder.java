package io.github.mfedenko09.roastlands.core.session;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class DailyQueueBuilder {

    private static final Comparator<CardState> OLDEST_DUE_FIRST =
            Comparator.comparing(CardState::dueDate).thenComparing(CardState::cardId);

    public DailyQueue build(
            Collection<CardState> states,
            List<String> newCandidates,
            LocalDate today,
            int maxReviews,
            int maxNew) {
        requireNotNegative(maxReviews, "maxReviews");
        requireNotNegative(maxNew, "maxNew");
        List<String> reviewCardIds = states.stream()
                .filter(state -> state.dueDate() != null && !state.dueDate().isAfter(today))
                .sorted(OLDEST_DUE_FIRST)
                .limit(maxReviews)
                .map(CardState::cardId)
                .toList();
        List<String> newCardIds = newCandidates.stream()
                .limit(maxNew)
                .toList();
        return new DailyQueue(reviewCardIds, newCardIds);
    }

    private static void requireNotNegative(int limit, String name) {
        if (limit < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + limit);
        }
    }
}
