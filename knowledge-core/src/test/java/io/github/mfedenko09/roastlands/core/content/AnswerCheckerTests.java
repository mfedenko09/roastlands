package io.github.mfedenko09.roastlands.core.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnswerCheckerTests {

    private final AnswerChecker checker = new AnswerChecker();

    @Test
    void codeOutputAndSingleChoiceAcceptOnlyTheMarkedIndex() {
        Question question = question(QuestionType.CODE_OUTPUT, new SingleIndex(1));

        assertThat(checker.isCorrect(question, Set.of(1))).isTrue();
        assertThat(checker.isCorrect(question, Set.of(0))).isFalse();
        assertThat(checker.isCorrect(question, Set.of(0, 1))).isFalse();
        assertThat(checker.isCorrect(question, Set.of())).isFalse();
    }

    @Test
    void singleChoiceUsesTheSameRule() {
        Question question = question(QuestionType.SINGLE_CHOICE, new SingleIndex(0));

        assertThat(checker.isCorrect(question, Set.of(0))).isTrue();
        assertThat(checker.isCorrect(question, Set.of(2))).isFalse();
    }

    @Test
    void multiChoiceNeedsExactlyTheMarkedSet() {
        Question question = question(QuestionType.MULTI_CHOICE, new IndexSet(Set.of(0, 2)));

        assertThat(checker.isCorrect(question, Set.of(2, 0))).isTrue();
        assertThat(checker.isCorrect(question, Set.of(0))).isFalse();
        assertThat(checker.isCorrect(question, Set.of(0, 1, 2))).isFalse();
        assertThat(checker.isCorrect(question, Set.of())).isFalse();
    }

    @Test
    void trueFalseComparesTheBoolean() {
        Question question = question(QuestionType.TRUE_FALSE, new Bool(false));

        assertThat(checker.isCorrect(question, false)).isTrue();
        assertThat(checker.isCorrect(question, true)).isFalse();
    }

    @Test
    void trueFalseQuestionCannotBeAnsweredWithIndexes() {
        Question question = question(QuestionType.TRUE_FALSE, new Bool(true));

        assertThatThrownBy(() -> checker.isCorrect(question, Set.of(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("core.types.sample is answered with true or false");
    }

    @Test
    void indexQuestionCannotBeAnsweredWithTrueOrFalse() {
        Question question = question(QuestionType.SINGLE_CHOICE, new SingleIndex(0));

        assertThatThrownBy(() -> checker.isCorrect(question, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("core.types.sample is answered with option indexes");
    }

    private static Question question(QuestionType type, CorrectAnswer correct) {
        return Question.builder()
                .id("core.types.sample")
                .topic("core.types")
                .type(type)
                .difficulty(1)
                .prompt("Prompt")
                .options(List.of(new TextOption("a"), new TextOption("b"), new TextOption("c")))
                .correct(correct)
                .explanation("Because")
                .reference("JLS §1.1")
                .minJavaVersion(21)
                .source("pack/questions/core.types.yaml")
                .build();
    }
}
