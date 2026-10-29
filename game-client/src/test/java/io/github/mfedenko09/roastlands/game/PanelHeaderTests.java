package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.session.SessionProgress;
import org.junit.jupiter.api.Test;

class PanelHeaderTests {

    @Test
    void showsTopicDifficultyAndProgress() {
        assertThat(PanelHeader.text("Generics", 3, new SessionProgress(4, 12)))
                .isEqualTo("Generics  |  Difficulty 3  |  4/12");
    }
}
