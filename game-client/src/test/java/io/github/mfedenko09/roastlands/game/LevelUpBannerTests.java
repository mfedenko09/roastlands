package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LevelUpBannerTests {

    @Test
    void levelWithACosmeticNamesTheNewLook() {
        assertThat(LevelUpBanner.text(List.of(3))).isEqualTo("Level 3 reached!\nNew look: Scarf");
    }

    @Test
    void levelWithATitleNamesTheNewTitle() {
        assertThat(LevelUpBanner.text(List.of(5))).isEqualTo("Level 5 reached!\nNew title: Middle");
    }

    @Test
    void levelWithoutARewardShowsOnlyTheLevel() {
        assertThat(LevelUpBanner.text(List.of(2))).isEqualTo("Level 2 reached!");
    }

    @Test
    void severalLevelsAreListedInOrderEachWithItsReward() {
        assertThat(LevelUpBanner.text(List.of(3, 4)))
                .isEqualTo("Level 3 reached!\nNew look: Scarf\nLevel 4 reached!\nNew look: Cap");
    }
}
