package io.github.mfedenko09.roastlands.core.answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.mfedenko09.roastlands.core.srs.Grade;
import org.junit.jupiter.api.Test;

class GradeResolverTests {

    private final GradeResolver resolver = new GradeResolver();

    @Test
    void wrongAnswerIsAgainWhateverTheConfidence() {
        assertThat(resolver.resolve(false, Confidence.HIGH)).isEqualTo(Grade.AGAIN);
        assertThat(resolver.resolve(false, Confidence.LOW)).isEqualTo(Grade.AGAIN);
    }

    @Test
    void wrongAnswerDoesNotNeedConfidence() {
        assertThat(resolver.resolve(false, null)).isEqualTo(Grade.AGAIN);
    }

    @Test
    void correctAnswerWithLowConfidenceIsHard() {
        assertThat(resolver.resolve(true, Confidence.LOW)).isEqualTo(Grade.HARD);
    }

    @Test
    void correctAnswerWithHighConfidenceIsGood() {
        assertThat(resolver.resolve(true, Confidence.HIGH)).isEqualTo(Grade.GOOD);
    }

    @Test
    void correctAnswerWithoutConfidenceIsRejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> resolver.resolve(true, null))
                .withMessage("confidence is required for a correct answer");
    }
}
