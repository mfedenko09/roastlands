package io.github.mfedenko09.roastlands.core.mastery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MasteryLevelTests {

    @ParameterizedTest
    @CsvSource({
        "0.0, RUSTY",
        "0.399, RUSTY",
        "0.4, WARM",
        "0.749, WARM",
        "0.75, SHARP",
        "1.0, SHARP"
    })
    void levelFollowsThresholdsForReviewedTopic(double mastery, MasteryLevel expected) {
        assertThat(MasteryLevel.of(mastery, true)).isEqualTo(expected);
    }

    @Test
    void topicWithoutReviewsIsNew() {
        assertThat(MasteryLevel.of(0.0, false)).isEqualTo(MasteryLevel.NEW);
    }
}
