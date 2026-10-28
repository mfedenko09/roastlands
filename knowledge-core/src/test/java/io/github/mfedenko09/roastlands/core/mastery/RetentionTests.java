package io.github.mfedenko09.roastlands.core.mastery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RetentionTests {

    private static final LocalDate REVIEWED_ON = LocalDate.of(2026, 11, 1);

    private final Retention retention = new Retention();

    @Test
    void neverReviewedCardHasNoRetention() {
        assertThat(retention.estimate(CardState.newCard("c1"), REVIEWED_ON)).isZero();
    }

    @Test
    void retentionIsFullRightAfterReview() {
        assertThat(retention.estimate(reviewedWithInterval(10), REVIEWED_ON)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void retentionIsNinetyPercentOnDueDate() {
        assertThat(retention.estimate(reviewedWithInterval(10), REVIEWED_ON.plusDays(10)))
                .isCloseTo(0.9, within(1e-9));
    }

    @Test
    void retentionKeepsFallingAfterDueDate() {
        assertThat(retention.estimate(reviewedWithInterval(10), REVIEWED_ON.plusDays(20)))
                .isCloseTo(0.81, within(1e-9));
    }

    @Test
    void retentionDoesNotExceedOneWhenTodayIsBeforeLastReview() {
        assertThat(retention.estimate(reviewedWithInterval(10), REVIEWED_ON.minusDays(5)))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void reviewedCardWithZeroIntervalIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> retention.estimate(reviewedWithInterval(0), REVIEWED_ON))
                .withMessage("reviewed card must have a positive interval: c1");
    }

    private static CardState reviewedWithInterval(int intervalDays) {
        return CardState.builder()
                .cardId("c1")
                .easeFactor(2.5)
                .intervalDays(intervalDays)
                .lastReviewDate(REVIEWED_ON)
                .dueDate(REVIEWED_ON.plusDays(intervalDays))
                .build();
    }
}
