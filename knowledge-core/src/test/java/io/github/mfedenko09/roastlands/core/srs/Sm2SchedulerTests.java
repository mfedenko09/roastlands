package io.github.mfedenko09.roastlands.core.srs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class Sm2SchedulerTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    private final Sm2Scheduler scheduler = new Sm2Scheduler();

    @Test
    void firstGoodAnswerOnNewCardSchedulesNextDay() {
        CardState reviewed = scheduler.review(CardState.newCard("c1"), Grade.GOOD, TODAY);

        assertThat(reviewed.repetitions()).isEqualTo(1);
        assertThat(reviewed.intervalDays()).isEqualTo(1);
        assertThat(reviewed.easeFactor()).isCloseTo(2.5, within(1e-9));
        assertThat(reviewed.dueDate()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(reviewed.lastReviewDate()).isEqualTo(TODAY);
        assertThat(reviewed.lapses()).isZero();
    }

    @Test
    void secondGoodAnswerSchedulesSixDays() {
        CardState afterFirst = cardWith(1, 2.5, 1, 0);

        CardState reviewed = scheduler.review(afterFirst, Grade.GOOD, TODAY);

        assertThat(reviewed.intervalDays()).isEqualTo(6);
        assertThat(reviewed.repetitions()).isEqualTo(2);
        assertThat(reviewed.dueDate()).isEqualTo(LocalDate.of(2026, 11, 7));
    }

    @Test
    void intervalGrowsByEaseFactorAfterRepeatedGoodAnswers() {
        CardState afterTwo = cardWith(2, 2.5, 6, 0);

        CardState reviewed = scheduler.review(afterTwo, Grade.GOOD, TODAY);

        assertThat(reviewed.intervalDays()).isEqualTo(15);
        assertThat(reviewed.repetitions()).isEqualTo(3);
        assertThat(reviewed.easeFactor()).isCloseTo(2.5, within(1e-9));
        assertThat(reviewed.dueDate()).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void grownIntervalIsRoundedToWholeDays() {
        CardState card = cardWith(3, 1.7, 7, 0);

        CardState reviewed = scheduler.review(card, Grade.GOOD, TODAY);

        assertThat(reviewed.intervalDays()).isEqualTo(12);
    }

    @Test
    void halfDayIntervalIsRoundedUp() {
        CardState card = cardWith(3, 2.5, 3, 0);

        CardState reviewed = scheduler.review(card, Grade.GOOD, TODAY);

        assertThat(reviewed.intervalDays()).isEqualTo(8);
    }

    @Test
    void wrongAnswerResetsCard() {
        CardState mature = cardWith(3, 2.5, 15, 0);

        CardState reviewed = scheduler.review(mature, Grade.AGAIN, TODAY);

        assertThat(reviewed.repetitions()).isZero();
        assertThat(reviewed.intervalDays()).isEqualTo(1);
        assertThat(reviewed.easeFactor()).isCloseTo(1.96, within(1e-9));
        assertThat(reviewed.lapses()).isEqualTo(1);
        assertThat(reviewed.dueDate()).isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    void lapsesAccumulate() {
        CardState reviewed = scheduler.review(cardWith(2, 2.5, 6, 4), Grade.AGAIN, TODAY);

        assertThat(reviewed.lapses()).isEqualTo(5);
    }

    @Test
    void easeFactorNeverDropsBelowFloor() {
        CardState card = cardWith(2, 1.4, 6, 0);

        CardState reviewed = scheduler.review(card, Grade.AGAIN, TODAY);

        assertThat(reviewed.easeFactor()).isCloseTo(1.3, within(1e-9));
    }

    @ParameterizedTest
    @CsvSource({"HARD, 2.36", "GOOD, 2.5", "EASY, 2.6"})
    void easeFactorChangeFollowsSm2Formula(Grade grade, double expectedEaseFactor) {
        CardState reviewed = scheduler.review(cardWith(2, 2.5, 6, 0), grade, TODAY);

        assertThat(reviewed.easeFactor()).isCloseTo(expectedEaseFactor, within(1e-9));
    }

    @Test
    void hardAnswerCountsAsPassed() {
        CardState reviewed = scheduler.review(cardWith(2, 2.5, 6, 0), Grade.HARD, TODAY);

        assertThat(reviewed.repetitions()).isEqualTo(3);
        assertThat(reviewed.lapses()).isZero();
        assertThat(reviewed.intervalDays()).isEqualTo(15);
    }

    @Test
    void reviewDoesNotChangeOriginalState() {
        CardState original = cardWith(2, 2.5, 6, 0);

        scheduler.review(original, Grade.AGAIN, TODAY);

        assertThat(original).isEqualTo(cardWith(2, 2.5, 6, 0));
    }

    @Test
    void newCardHasInitialValues() {
        CardState card = CardState.newCard("c1");

        assertThat(card.cardId()).isEqualTo("c1");
        assertThat(card.repetitions()).isZero();
        assertThat(card.easeFactor()).isEqualTo(2.5);
        assertThat(card.intervalDays()).isZero();
        assertThat(card.dueDate()).isNull();
        assertThat(card.lastReviewDate()).isNull();
        assertThat(card.lapses()).isZero();
    }

    @Test
    void firstReviewOfNewCardRemembersTheDayAndLaterReviewsKeepIt() {
        CardState first = scheduler.review(CardState.newCard("c1"), Grade.AGAIN, TODAY);
        CardState later = scheduler.review(first, Grade.GOOD, TODAY.plusDays(1));

        assertThat(first.firstSeenDate()).isEqualTo(TODAY);
        assertThat(later.firstSeenDate()).isEqualTo(TODAY);
    }

    @Test
    void cardReviewedBeforeFirstSeenDateExistedStaysWithoutOne() {
        CardState legacy = cardWith(1, 2.5, 1, 0).toBuilder().lastReviewDate(TODAY.minusDays(1)).build();

        assertThat(scheduler.review(legacy, Grade.GOOD, TODAY).firstSeenDate()).isNull();
    }

    private static CardState cardWith(int repetitions, double easeFactor, int intervalDays, int lapses) {
        return CardState.builder()
                .cardId("c1")
                .repetitions(repetitions)
                .easeFactor(easeFactor)
                .intervalDays(intervalDays)
                .lapses(lapses)
                .build();
    }
}
