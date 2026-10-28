package io.github.mfedenko09.roastlands.core.srs;

import java.time.LocalDate;
import lombok.Builder;

@Builder(toBuilder = true)
public record CardState(
        String cardId,
        int repetitions,
        double easeFactor,
        int intervalDays,
        LocalDate dueDate,
        LocalDate lastReviewDate,
        int lapses) {

    public static final double INITIAL_EASE_FACTOR = 2.5;

    public static CardState newCard(String cardId) {
        return CardState.builder()
                .cardId(cardId)
                .easeFactor(INITIAL_EASE_FACTOR)
                .build();
    }
}
