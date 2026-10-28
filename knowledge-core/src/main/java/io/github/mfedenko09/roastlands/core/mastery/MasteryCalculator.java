package io.github.mfedenko09.roastlands.core.mastery;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.time.LocalDate;
import java.util.Collection;

public final class MasteryCalculator {

    private final Retention retention = new Retention();

    public double topicMastery(Collection<CardState> reviewed, int totalCards, LocalDate today) {
        if (reviewed.size() > totalCards) {
            throw new IllegalArgumentException(
                    "reviewed cards (" + reviewed.size() + ") exceed total cards (" + totalCards + ")");
        }
        if (totalCards == 0) {
            return 0;
        }
        double retentionSum = reviewed.stream()
                .mapToDouble(state -> retention.estimate(state, today))
                .sum();
        return retentionSum / totalCards;
    }
}
