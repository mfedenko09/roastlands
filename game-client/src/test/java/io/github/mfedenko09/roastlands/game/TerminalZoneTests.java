package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

class TerminalZoneTests {

    private static final Vector3f TERMINAL = new Vector3f(5f, 2f, 0f);

    @Test
    void playerExactlyTwoMetersAwayIsInside() {
        assertThat(TerminalZone.contains(TERMINAL, new Vector3f(7f, 2f, 0f))).isTrue();
    }

    @Test
    void playerJustBeyondTwoMetersIsOutside() {
        assertThat(TerminalZone.contains(TERMINAL, new Vector3f(7.01f, 2f, 0f))).isFalse();
    }

    @Test
    void heightIsIgnoredAndBothAxesCount() {
        assertThat(TerminalZone.contains(TERMINAL, new Vector3f(6f, 9f, 1f))).isTrue();
        assertThat(TerminalZone.contains(TERMINAL, new Vector3f(6.5f, 2f, 1.5f))).isFalse();
    }
}
