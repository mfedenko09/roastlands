package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class NextReview {

    Optional<LocalDate> after(Collection<CardState> cards, Collection<Question> questions, LocalDate today) {
        Set<String> knownIds = questions.stream().map(Question::id).collect(Collectors.toSet());
        return cards.stream()
                .filter(card -> knownIds.contains(card.cardId()))
                .map(CardState::dueDate)
                .filter(due -> due != null && due.isAfter(today))
                .min(LocalDate::compareTo);
    }
}
