package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;

class ErrorReasonTests {

    @Test
    void usesTheDeepestMessage() {
        Exception error = new UncheckedIOException("Cannot save profile", new IOException("Access is denied"));

        assertThat(ErrorReason.of(error)).isEqualTo("Access is denied");
    }

    @Test
    void usesTheOuterMessageWhenTheCauseHasNone() {
        Exception error = new IllegalStateException("disk full", new RuntimeException((String) null));

        assertThat(ErrorReason.of(error)).isEqualTo("disk full");
    }

    @Test
    void fallsBackToTheClassNameWhenNothingHasAMessage() {
        assertThat(ErrorReason.of(new IllegalStateException())).isEqualTo("IllegalStateException");
    }

    @Test
    void blankMessagesAreSkipped() {
        assertThat(ErrorReason.of(new IllegalStateException("  "))).isEqualTo("IllegalStateException");
    }
}
