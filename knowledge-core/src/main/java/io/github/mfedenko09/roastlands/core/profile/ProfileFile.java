package io.github.mfedenko09.roastlands.core.profile;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

record ProfileFile(
        Integer schemaVersion,
        List<CardState> cards,
        Set<String> unlockedTopics,
        Streak streak,
        Long xp,
        Set<String> unlockedRewards) {

    static ProfileFile of(PlayerProfile profile) {
        List<CardState> sortedCards = profile.cards().values().stream()
                .sorted(Comparator.comparing(CardState::cardId))
                .toList();
        return new ProfileFile(
                profile.schemaVersion(),
                sortedCards,
                new TreeSet<>(profile.unlockedTopics()),
                profile.streak(),
                profile.xp(),
                new TreeSet<>(profile.unlockedRewards()));
    }

    boolean isComplete() {
        return schemaVersion != null && cards != null && unlockedTopics != null && streak != null && xp != null
                && unlockedRewards != null && !cards.contains(null) && !unlockedTopics.contains(null)
                && !unlockedRewards.contains(null);
    }

    PlayerProfile toProfile() {
        Map<String, CardState> cardsById = new HashMap<>();
        for (CardState card : cards) {
            if (card.cardId() == null || cardsById.put(card.cardId(), card) != null) {
                throw new IllegalArgumentException("Card id is missing or duplicated: " + card.cardId());
            }
        }
        return PlayerProfile.builder()
                .schemaVersion(schemaVersion)
                .cards(cardsById)
                .unlockedTopics(unlockedTopics)
                .streak(streak)
                .xp(xp)
                .unlockedRewards(unlockedRewards)
                .build();
    }
}
