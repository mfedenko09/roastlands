package io.github.mfedenko09.roastlands.core.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlayerProfileTests {

    @Test
    void emptyProfileUnlocksRootsOnly() {
        PlayerProfile empty = PlayerProfile.empty(Set.of("core.types"));

        assertThat(empty).isEqualTo(PlayerProfile.builder()
                .schemaVersion(1)
                .cards(Map.of())
                .unlockedTopics(Set.of("core.types"))
                .streak(new Streak(0, 0, null))
                .xp(0)
                .build());
    }

    @Test
    void cardStoredUnderAnotherIdIsRejected() {
        assertThatThrownBy(() -> PlayerProfile.builder()
                        .schemaVersion(1)
                        .cards(Map.of("a", CardState.newCard("b")))
                        .unlockedTopics(Set.of())
                        .streak(Streak.none())
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"a\"")
                .hasMessageContaining("\"b\"");
    }
}
