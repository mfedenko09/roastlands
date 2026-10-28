package io.github.mfedenko09.roastlands.core.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyQueueBuilderTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    private final DailyQueueBuilder builder = new DailyQueueBuilder();

    @Test
    void queueIsCappedAndDueCardsComeBeforeNewOnes() {
        List<CardState> states = new ArrayList<>();
        for (int day = 1; day <= 40; day++) {
            states.add(dueOn(String.format("c%02d", day), TODAY.minusDays(day)));
        }
        List<String> newCandidates = List.of("n1", "n2", "n3", "n4", "n5", "n6", "n7", "n8", "n9", "n10");

        DailyQueue queue = builder.build(states, newCandidates, TODAY, 15, 5);

        assertThat(queue.reviewCardIds()).hasSize(15)
                .first().isEqualTo("c40");
        assertThat(queue.reviewCardIds()).last().isEqualTo("c26");
        assertThat(queue.newCardIds()).containsExactly("n1", "n2", "n3", "n4", "n5");
    }

    @Test
    void sameDueDateIsOrderedByCardId() {
        List<CardState> states = List.of(
                dueOn("b", TODAY.minusDays(1)),
                dueOn("a", TODAY.minusDays(1)),
                dueOn("c", TODAY.minusDays(2)));

        DailyQueue queue = builder.build(states, List.of(), TODAY, 10, 0);

        assertThat(queue.reviewCardIds()).containsExactly("c", "a", "b");
    }

    @Test
    void cardDueTodayIsIncludedAndCardDueTomorrowIsNot() {
        List<CardState> states = List.of(dueOn("today", TODAY), dueOn("tomorrow", TODAY.plusDays(1)));

        DailyQueue queue = builder.build(states, List.of(), TODAY, 10, 0);

        assertThat(queue.reviewCardIds()).containsExactly("today");
    }

    @Test
    void cardsWithoutDueDateAreNotReviews() {
        DailyQueue queue = builder.build(List.of(CardState.newCard("fresh")), List.of(), TODAY, 10, 0);

        assertThat(queue.reviewCardIds()).isEmpty();
    }

    @Test
    void newCardsKeepOrderGivenByCaller() {
        DailyQueue queue = builder.build(List.of(), List.of("z", "a", "m"), TODAY, 10, 2);

        assertThat(queue.newCardIds()).containsExactly("z", "a");
    }

    @Test
    void zeroLimitsGiveEmptyQueue() {
        DailyQueue queue = builder.build(List.of(dueOn("c", TODAY)), List.of("n"), TODAY, 0, 0);

        assertThat(queue.reviewCardIds()).isEmpty();
        assertThat(queue.newCardIds()).isEmpty();
    }

    @Test
    void negativeMaxReviewsIsRejected() {
        assertThatThrownBy(() -> builder.build(List.of(), List.of(), TODAY, -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxReviews must not be negative: -1");
    }

    @Test
    void negativeMaxNewIsRejected() {
        assertThatThrownBy(() -> builder.build(List.of(), List.of(), TODAY, 0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxNew must not be negative: -1");
    }

    @Test
    void queueListsAreImmutable() {
        DailyQueue queue = builder.build(List.of(dueOn("c", TODAY)), List.of("n"), TODAY, 10, 10);

        assertThatThrownBy(() -> queue.reviewCardIds().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> queue.newCardIds().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static CardState dueOn(String cardId, LocalDate dueDate) {
        return CardState.builder()
                .cardId(cardId)
                .easeFactor(2.5)
                .intervalDays(1)
                .dueDate(dueDate)
                .lastReviewDate(dueDate.minusDays(1))
                .build();
    }
}
