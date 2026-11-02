package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.Bool;
import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.topic.Island;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoverageReportTests {

    private final TopicGraph topics = TopicGraph.of(
            List.of(new Island("shore", "Shore")),
            List.of(topic("a", "one", "two"), topic("b", "three")));

    @Test
    void listsEveryKeyPointWithItsQuestionsAcrossPacks() {
        ContentPack first = new ContentPack("first", List.of(question("a.x", "a", 0), question("b.x", "b", 0)));
        ContentPack second = new ContentPack("second", List.of(question("a.y", "a", 0), question("a.z", "a", 1)));

        assertThat(CoverageReport.render(topics, List.of(first, second))).isEqualTo("""
                a (questions: 3)
                  0 one: a.x, a.y
                  1 two: a.z
                b (questions: 1)
                  0 three: b.x
                Uncovered key points: 0
                """);
    }

    @Test
    void listsUncoveredKeyPointsAndTopicsWithoutQuestions() {
        assertThat(CoverageReport.render(topics, List.of(new ContentPack("only", List.of(question("a.x", "a", 1))))))
                .isEqualTo("""
                        a (questions: 1)
                          0 one: -
                          1 two: a.x
                        b (questions: 0)
                          0 three: -
                        Uncovered key points: 2
                          a 0 one
                          b 0 three
                        """);
    }

    private static Topic topic(String id, String... keyPoints) {
        return Topic.builder()
                .id(id)
                .title(id)
                .islandId("shore")
                .prerequisites(List.of())
                .minJavaVersion(21)
                .keyPoints(List.of(keyPoints))
                .build();
    }

    private static Question question(String id, String topic, int keyPoint) {
        return Question.builder()
                .id(id)
                .topic(topic)
                .type(QuestionType.TRUE_FALSE)
                .difficulty(1)
                .keyPoint(keyPoint)
                .prompt("Is it so?")
                .options(List.of())
                .correct(new Bool(true))
                .explanation("Because.")
                .reference("JLS §1.1")
                .minJavaVersion(21)
                .source("test")
                .build();
    }
}
