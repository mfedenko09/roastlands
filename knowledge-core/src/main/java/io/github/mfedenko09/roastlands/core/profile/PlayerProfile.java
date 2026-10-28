package io.github.mfedenko09.roastlands.core.profile;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

@Builder(toBuilder = true)
public record PlayerProfile(
        int schemaVersion, Map<String, CardState> cards, Set<String> unlockedTopics, Streak streak, long xp) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PlayerProfile {
        cards = Map.copyOf(cards);
        unlockedTopics = Set.copyOf(unlockedTopics);
        cards.forEach((id, card) -> {
            if (!id.equals(card.cardId())) {
                throw new IllegalArgumentException("Card stored under \"" + id + "\" has id \"" + card.cardId() + "\"");
            }
        });
    }

    public static PlayerProfile empty(Set<String> roots) {
        return PlayerProfile.builder()
                .schemaVersion(CURRENT_SCHEMA_VERSION)
                .cards(Map.of())
                .unlockedTopics(roots)
                .streak(Streak.none())
                .build();
    }
}
