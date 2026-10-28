package io.github.mfedenko09.roastlands.core.session;

import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.mastery.MasteryCalculator;
import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class MasteryBoard {

    private final MasteryCalculator calculator = new MasteryCalculator();
    private final Map<String, String> topicByQuestionId;
    private final Map<String, Integer> questionCountByTopic;

    MasteryBoard(Collection<Question> questions) {
        topicByQuestionId = questions.stream().collect(Collectors.toMap(Question::id, Question::topic));
        questionCountByTopic = questions.stream()
                .collect(Collectors.groupingBy(Question::topic, Collectors.summingInt(question -> 1)));
    }

    Map<String, Double> masteryByTopic(Map<String, CardState> cards, LocalDate today) {
        Map<String, List<CardState>> reviewedByTopic = new HashMap<>();
        cards.values().forEach(card -> {
            String topic = topicByQuestionId.get(card.cardId());
            if (topic != null) {
                reviewedByTopic.computeIfAbsent(topic, key -> new ArrayList<>()).add(card);
            }
        });
        Map<String, Double> mastery = new HashMap<>();
        questionCountByTopic.forEach((topic, total) -> mastery.put(
                topic, calculator.topicMastery(reviewedByTopic.getOrDefault(topic, List.of()), total, today)));
        return mastery;
    }
}
