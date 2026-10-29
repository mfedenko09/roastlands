package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ActionTests {

    @Test
    void optionKeysMapToZeroBasedIndexes() {
        assertThat(Action.OPTION_1.optionIndex()).hasValue(0);
        assertThat(Action.OPTION_5.optionIndex()).hasValue(4);
    }

    @Test
    void otherActionsAreNotOptions() {
        assertThat(Action.SURE.optionIndex()).isEmpty();
    }

    @Test
    void panelKeysDoNotMovePlayer() {
        assertThat(Action.PLAYER_CONTROLS).contains(Action.BACK, Action.JUMP)
                .doesNotContain(Action.SURE, Action.INTERACT, Action.CANCEL);
    }
}
