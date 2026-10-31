package io.github.mfedenko09.roastlands.core.progress;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.profile.Streak;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StreakRulesTests {

    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_2 = LocalDate.of(2026, 11, 2);

    private final StreakRules rules = new StreakRules();

    @Test
    void firstCompletedQuestStartsTheStreak() {
        assertThat(rules.onQuestCompleted(Streak.none(), NOV_2)).isEqualTo(new Streak(1, 1, NOV_2));
    }

    @Test
    void nextDayContinuesTheStreakAndRaisesBest() {
        assertThat(rules.onQuestCompleted(new Streak(4, 4, NOV_1), NOV_2)).isEqualTo(new Streak(5, 5, NOV_2));
    }

    @Test
    void nextDayKeepsAHigherBest() {
        assertThat(rules.onQuestCompleted(new Streak(4, 6, NOV_1), NOV_2)).isEqualTo(new Streak(5, 6, NOV_2));
    }

    @Test
    void aSkippedDayBreaksTheStreakAndKeepsBest() {
        Streak streak = new Streak(4, 6, LocalDate.of(2026, 10, 30));

        assertThat(rules.onQuestCompleted(streak, NOV_2)).isEqualTo(new Streak(1, 6, NOV_2));
    }

    @Test
    void streakContinuesOverTheEndOfTheMonth() {
        Streak streak = new Streak(2, 2, LocalDate.of(2026, 10, 31));

        assertThat(rules.onQuestCompleted(streak, NOV_1)).isEqualTo(new Streak(3, 3, NOV_1));
    }

    @Test
    void secondCompletionOnTheSameDayChangesNothing() {
        Streak streak = new Streak(5, 5, NOV_2);

        assertThat(rules.onQuestCompleted(streak, NOV_2)).isEqualTo(streak);
    }

    @Test
    void clockMovedBackChangesNothing() {
        Streak streak = new Streak(5, 5, NOV_2);

        assertThat(rules.onQuestCompleted(streak, NOV_1)).isEqualTo(streak);
    }

    @Test
    void streakCompletedTodayOrYesterdayIsStillAlive() {
        assertThat(rules.currentOn(new Streak(5, 5, NOV_2), NOV_2)).isEqualTo(5);
        assertThat(rules.currentOn(new Streak(5, 5, NOV_1), NOV_2)).isEqualTo(5);
    }

    @Test
    void streakWithASkippedDayIsZeroAndTheStoredValueStays() {
        Streak streak = new Streak(5, 7, LocalDate.of(2026, 10, 31));

        assertThat(rules.currentOn(streak, NOV_2)).isZero();
        assertThat(streak.current()).isEqualTo(5);
    }

    @Test
    void noCompletedQuestIsZero() {
        assertThat(rules.currentOn(Streak.none(), NOV_2)).isZero();
    }

    @Test
    void clockMovedBackKeepsTheStreak() {
        assertThat(rules.currentOn(new Streak(5, 5, NOV_2), NOV_1)).isEqualTo(5);
    }
}
