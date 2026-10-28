package io.github.mfedenko09.roastlands.core.session;

import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.profile.PlayerProfile;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class NewQuestionPicker {

    private final Map<String, Integer> topicOrder = new HashMap<>();

    NewQuestionPicker(TopicGraph topics) {
        List<Topic> ordered = topics.topologicalOrder();
        for (int position = 0; position < ordered.size(); position++) {
            topicOrder.put(ordered.get(position).id(), position);
        }
    }

    List<String> candidates(Collection<Question> questions, PlayerProfile profile) {
        Comparator<Question> mapOrder = Comparator.comparingInt((Question question) -> topicOrder.get(question.topic()))
                .thenComparingInt(Question::difficulty)
                .thenComparing(Question::id);
        return questions.stream()
                .filter(question -> topicOrder.containsKey(question.topic()))
                .filter(question -> profile.unlockedTopics().contains(question.topic()))
                .filter(question -> !profile.cards().containsKey(question.id()))
                .sorted(mapOrder)
                .map(Question::id)
                .toList();
    }
}
