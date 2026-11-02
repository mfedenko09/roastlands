package io.github.mfedenko09.roastlands.core.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TitlesTests {

    private final Titles titles = new Titles();

    @ParameterizedTest
    @CsvSource({
        "1, Junior", "4, Junior", "5, Middle", "9, Middle", "10, Senior", "19, Senior", "20, Staff", "99, Staff"
    })
    void titleFollowsTheLevelRanges(int level, String title) {
        assertThat(titles.of(level)).isEqualTo(title);
    }

    @Test
    void levelBelowOneIsRejected() {
        assertThatThrownBy(() -> titles.of(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
