package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import io.github.mfedenko09.roastlands.core.session.AnswerResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class DayTallyTests {

    @Test
    void emptyDayHasZeroPercent() {
        assertThat(new DayTally().summary()).isEqualTo(new DaySummary(0, 0, 0, List.of()));
    }

    @Test
    void percentIsRoundedAndUnlockedTopicsAreDistinctInOrder() {
        DayTally tally = new DayTally();
        tally.record(result(true, "a", "b"));
        tally.record(result(true, "b"));
        tally.record(result(false));

        assertThat(tally.summary()).isEqualTo(new DaySummary(3, 2, 67, List.of("a", "b")));
    }

    @Test
    void replacingTheLastResultUsesItsUnlockedTopics() {
        DayTally tally = new DayTally();
        tally.record(result(true));
        tally.replaceLast(result(true, "c"));

        assertThat(tally.summary()).isEqualTo(new DaySummary(1, 1, 100, List.of("c")));
    }

    private static AnswerResult result(boolean correct, String... unlocked) {
        return AnswerResult.builder()
                .correct(correct)
                .correctAnswer(new SingleIndex(0))
                .topicsUnlocked(List.of(unlocked))
                .build();
    }
}
