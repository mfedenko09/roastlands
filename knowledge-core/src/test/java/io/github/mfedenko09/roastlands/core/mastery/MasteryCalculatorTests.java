package io.github.mfedenko09.roastlands.core.mastery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MasteryCalculatorTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

    private final MasteryCalculator calculator = new MasteryCalculator();

    @Test
    void masteryDecaysWhenTopicIsNotReviewed() {
        CardState reviewed = reviewedTwentyDaysAgoWithInterval(10);

        double mastery = calculator.topicMastery(List.of(reviewed), 2, TODAY);

        assertThat(mastery).isCloseTo(0.405, within(1e-9));
    }

    @Test
    void masteryAveragesRetentionOverAllCardsInTopic() {
        CardState first = reviewedTwentyDaysAgoWithInterval(10);
        CardState second = reviewedTwentyDaysAgoWithInterval(20);

        double mastery = calculator.topicMastery(List.of(first, second), 4, TODAY);

        assertThat(mastery).isCloseTo((0.81 + 0.9) / 4, within(1e-9));
    }

    @Test
    void topicWithoutCardsHasZeroMastery() {
        assertThat(calculator.topicMastery(List.of(), 0, TODAY)).isZero();
    }

    @Test
    void topicWithoutReviewsHasZeroMastery() {
        assertThat(calculator.topicMastery(List.of(), 5, TODAY)).isZero();
    }

    @Test
    void moreReviewedCardsThanTotalIsRejected() {
        List<CardState> reviewed = List.of(
                reviewedTwentyDaysAgoWithInterval(10), reviewedTwentyDaysAgoWithInterval(20));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> calculator.topicMastery(reviewed, 1, TODAY))
                .withMessage("reviewed cards (2) exceed total cards (1)");
    }

    @Test
    void reviewedCountEqualToTotalIsAccepted() {
        double mastery = calculator.topicMastery(List.of(reviewedTwentyDaysAgoWithInterval(20)), 1, TODAY);

        assertThat(mastery).isCloseTo(0.9, within(1e-9));
    }

    private static CardState reviewedTwentyDaysAgoWithInterval(int intervalDays) {
        LocalDate reviewedOn = TODAY.minusDays(20);
        return CardState.builder()
                .cardId("c" + intervalDays)
                .easeFactor(2.5)
                .intervalDays(intervalDays)
                .lastReviewDate(reviewedOn)
                .dueDate(reviewedOn.plusDays(intervalDays))
                .build();
    }
}
