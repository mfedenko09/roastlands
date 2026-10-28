package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.CompileErrorOption;
import io.github.mfedenko09.roastlands.core.content.ContentPackLoader;
import io.github.mfedenko09.roastlands.core.content.ExceptionOption;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import io.github.mfedenko09.roastlands.core.topic.TopicMapLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class JavaCoreQuestionsTests {

    private static final Path PACKS_ROOT = Path.of("..", "content", "packs");

    private final List<Question> questions = new ContentPackLoader()
            .load(PACKS_ROOT.resolve("java-core"), new TopicMapLoader().load(PACKS_ROOT))
            .questions();

    @Test
    void firstPackHasThirtyQuestionsSpreadOverTenTopics() {
        Map<String, Long> perTopic = questions.stream()
                .collect(Collectors.groupingBy(Question::topic, Collectors.counting()));

        assertThat(perTopic).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("core.types", 4L), Map.entry("core.strings", 4L), Map.entry("core.oop", 4L),
                Map.entry("core.object-contract", 3L), Map.entry("core.exceptions", 4L),
                Map.entry("core.nested-classes", 1L), Map.entry("generics.basics", 2L),
                Map.entry("collections.lists", 2L), Map.entry("collections.hashing", 3L),
                Map.entry("collections.iteration", 3L)));
    }

    @Test
    void everyQuestionTypeIsUsedAndMostQuestionsAreCodeOutput() {
        Map<QuestionType, Long> perType = questions.stream()
                .collect(Collectors.groupingBy(Question::type, Collectors.counting()));

        assertThat(perType.get(QuestionType.CODE_OUTPUT)).isGreaterThanOrEqualTo(20);
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
    void difficultyMixIsAboutEightFourteenEight() {
        Map<Integer, Long> perDifficulty = questions.stream()
                .collect(Collectors.groupingBy(Question::difficulty, Collectors.counting()));

        assertThat(perDifficulty.get(1)).isBetween(7L, 9L);
        assertThat(perDifficulty.get(2)).isBetween(13L, 15L);
        assertThat(perDifficulty.get(3)).isBetween(7L, 9L);
    }

    @Test
    void idsAreUnique() {
        assertThat(questions).extracting(Question::id).doesNotHaveDuplicates();
    }

    private static Object markedOption(Question question) {
        return question.options().get(((SingleIndex) question.correct()).index());
    }
}
