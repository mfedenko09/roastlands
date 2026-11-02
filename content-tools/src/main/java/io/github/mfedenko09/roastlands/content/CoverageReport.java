package io.github.mfedenko09.roastlands.content;

import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class CoverageReport {

    private static final String NO_QUESTIONS = "-";

    private CoverageReport() {
    }

    static String render(TopicGraph topics, List<ContentPack> packs) {
        Map<String, List<Question>> questionsByTopic = packs.stream()
                .flatMap(pack -> pack.questions().stream())
                .collect(Collectors.groupingBy(Question::topic));
        StringBuilder report = new StringBuilder();
        List<String> uncovered = new ArrayList<>();
        for (Topic topic : topics.topics()) {
            List<Question> questions = questionsByTopic.getOrDefault(topic.id(), List.of());
            report.append(topic.id()).append(" (questions: ").append(questions.size()).append(")\n");
            for (int keyPoint = 0; keyPoint < topic.keyPoints().size(); keyPoint++) {
                String line = keyPoint + " " + topic.keyPoints().get(keyPoint);
                String covering = questionIds(questions, keyPoint);
                report.append("  ").append(line).append(": ").append(covering).append('\n');
                if (covering.equals(NO_QUESTIONS)) {
                    uncovered.add(topic.id() + " " + line);
                }
            }
        }
        report.append("Uncovered key points: ").append(uncovered.size()).append('\n');
        uncovered.forEach(line -> report.append("  ").append(line).append('\n'));
        return report.toString();
    }

    private static String questionIds(List<Question> questions, int keyPoint) {
        List<String> ids = questions.stream()
                .filter(question -> question.keyPoint() == keyPoint)
                .map(Question::id)
                .toList();
        return ids.isEmpty() ? NO_QUESTIONS : String.join(", ", ids);
    }
}
