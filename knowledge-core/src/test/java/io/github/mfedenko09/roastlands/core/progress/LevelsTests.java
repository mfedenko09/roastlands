package io.github.mfedenko09.roastlands.core.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LevelsTests {

    private final Levels levels = new Levels();

    @ParameterizedTest
    @CsvSource({"0, 1", "99, 1", "100, 2", "399, 2", "400, 3", "899, 3", "900, 4"})
    void levelFollowsTheSquareRootOfXp(long xp, int level) {
        assertThat(levels.levelOf(xp)).isEqualTo(level);
    }

    @ParameterizedTest
    @CsvSource({"1, 0", "2, 100", "3, 400", "4, 900"})
    void xpForLevelIsTheFirstXpOfThatLevel(int level, long xp) {
        assertThat(levels.xpForLevel(level)).isEqualTo(xp);
    }

    @Test
    void levelBelowOneIsRejected() {
        assertThatThrownBy(() -> levels.xpForLevel(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reachedListsEveryLevelPassedInOrder() {
        assertThat(levels.reached(399, 419)).containsExactly(3);
        assertThat(levels.reached(399, 900)).containsExactly(3, 4);
    }

    @Test
    void reachedIsEmptyWithinTheSameLevelOrWhenXpDoesNotGrow() {
        assertThat(levels.reached(400, 899)).isEqualTo(List.of());
        assertThat(levels.reached(900, 400)).isEmpty();
    }
}
