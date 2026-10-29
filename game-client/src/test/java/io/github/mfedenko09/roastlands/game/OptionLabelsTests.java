package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.CompileErrorOption;
import io.github.mfedenko09.roastlands.core.content.ExceptionOption;
import io.github.mfedenko09.roastlands.core.content.Option;
import io.github.mfedenko09.roastlands.core.content.OutputOption;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.TextOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class OptionLabelsTests {

    @Test
    void textOptionIsShownAsWritten() {
        assertThat(OptionLabels.label(new TextOption("final prevents reassignment")))
                .isEqualTo("final prevents reassignment");
    }

    @Test
    void outputOptionStartsWithPrintsAndJoinsLines() {
        assertThat(OptionLabels.label(new OutputOption("value\nfalse\n1\n"))).isEqualTo("Prints: value / false / 1");
    }

    @Test
    void compileErrorOptionSaysItDoesNotCompile() {
        assertThat(OptionLabels.label(new CompileErrorOption())).isEqualTo("Does not compile");
    }

    @Test
    void exceptionOptionWithoutOutputSaysThrows() {
        assertThat(OptionLabels.label(new ExceptionOption("NullPointerException", null)))
                .isEqualTo("Throws NullPointerException");
    }

    @Test
    void exceptionOptionWithOutputShowsWhatIsPrintedFirst() {
        assertThat(OptionLabels.label(new ExceptionOption("IllegalStateException", "a\nb")))
                .isEqualTo("Prints: a / b, then throws IllegalStateException");
    }

    @Test
    void trueFalseQuestionGetsTwoFixedLabels() {
        Question question = question(QuestionType.TRUE_FALSE, List.of());

        assertThat(OptionLabels.forQuestion(question)).containsExactly("True", "False");
    }

    @Test
    void choiceQuestionKeepsOptionOrder() {
        Question question = question(QuestionType.SINGLE_CHOICE, List.of(new TextOption("x"), new TextOption("y")));

        assertThat(OptionLabels.forQuestion(question)).containsExactly("x", "y");
    }

    private static Question question(QuestionType type, List<Option> options) {
        return Question.builder().type(type).options(options).build();
    }
}
