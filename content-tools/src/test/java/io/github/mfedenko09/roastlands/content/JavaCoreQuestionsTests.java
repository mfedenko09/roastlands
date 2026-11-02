package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.CompileErrorOption;
import io.github.mfedenko09.roastlands.core.content.ContentPackLoader;
import io.github.mfedenko09.roastlands.core.content.ExceptionOption;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import io.github.mfedenko09.roastlands.core.topic.TopicMapLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class JavaCoreQuestionsTests {

    private static final Path PACKS_ROOT = Path.of("..", "content", "packs");

    private static final int MIN_QUESTIONS_PER_TOPIC = 5;

    private static final int RARE_LEVEL_DIVISOR = 6;

    private static final Set<String> FINISHED_TOPICS = Set.of("core.nested-classes", "core.modern-types");

    private final TopicGraph topics = new TopicMapLoader().load(PACKS_ROOT);

    private final List<Question> questions = new ContentPackLoader()
            .load(PACKS_ROOT.resolve("java-core"), topics)
            .questions();

    @Test
    void finishedTopicsHaveEnoughQuestionsAndEveryKeyPointCovered() {
        Map<String, List<Question>> perTopic = questions.stream().collect(Collectors.groupingBy(Question::topic));

        assertThat(topics.topics()).filteredOn(topic -> FINISHED_TOPICS.contains(topic.id())).allSatisfy(topic -> {
            List<Question> own = perTopic.get(topic.id());
            assertThat(own).as(topic.id()).hasSizeGreaterThanOrEqualTo(MIN_QUESTIONS_PER_TOPIC);
            assertThat(own.stream().map(Question::keyPoint).distinct().count()).as(topic.id())
                    .isEqualTo(topic.keyPoints().size());
        });
    }

    @Test
    void everyQuestionTypeIsUsedAndMostQuestionsAreCodeOutput() {
        Map<QuestionType, Long> perType = questions.stream()
                .collect(Collectors.groupingBy(Question::type, Collectors.counting()));

        assertThat(perType.get(QuestionType.CODE_OUTPUT) * 3).isGreaterThanOrEqualTo(questions.size() * 2L);
        assertThat(perType.get(QuestionType.SINGLE_CHOICE)).isGreaterThanOrEqualTo(1);
        assertThat(perType.get(QuestionType.MULTI_CHOICE)).isGreaterThanOrEqualTo(1);
        assertThat(perType.get(QuestionType.TRUE_FALSE)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void compileErrorAndExceptionAreOftenTheRightAnswer() {
        List<Question> codeOutput = questions.stream().filter(q -> q.type() == QuestionType.CODE_OUTPUT).toList();

        assertThat(codeOutput.stream().filter(q -> markedOption(q) instanceof CompileErrorOption).count())
                .isGreaterThanOrEqualTo(3);
        assertThat(codeOutput.stream().filter(q -> markedOption(q) instanceof ExceptionOption).count())
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    void mediumIsTheLargestDifficultyAndNoLevelIsRare() {
        Map<Integer, Long> perDifficulty = questions.stream()
                .collect(Collectors.groupingBy(Question::difficulty, Collectors.counting()));

        assertThat(perDifficulty.get(2)).isGreaterThan(perDifficulty.get(1)).isGreaterThan(perDifficulty.get(3));
        assertThat(perDifficulty.get(1) * RARE_LEVEL_DIVISOR).isGreaterThanOrEqualTo(questions.size());
        assertThat(perDifficulty.get(3) * RARE_LEVEL_DIVISOR).isGreaterThanOrEqualTo(questions.size());
    }

    @Test
    void idsAreUnique() {
        assertThat(questions).extracting(Question::id).doesNotHaveDuplicates();
    }

    private static Object markedOption(Question question) {
        return question.options().get(((SingleIndex) question.correct()).index());
    }
}
