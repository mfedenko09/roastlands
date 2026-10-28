package io.github.mfedenko09.roastlands.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.CompileErrorOption;
import io.github.mfedenko09.roastlands.core.content.ExceptionOption;
import io.github.mfedenko09.roastlands.core.content.OutputOption;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import io.github.mfedenko09.roastlands.core.content.TextOption;
import io.github.mfedenko09.roastlands.core.session.AnswerResult;
import io.github.mfedenko09.roastlands.core.session.SessionProgress;
import java.util.List;
import org.junit.jupiter.api.Test;

class OptionAndFeedbackFormatterTests {

    private final OptionFormatter optionFormatter = new OptionFormatter();

    @Test
    void optionsAreDescribedByTheirKind() {
        assertThat(optionFormatter.describe(new TextOption("final"))).isEqualTo("final");
        assertThat(optionFormatter.describe(new OutputOption("a\nb"))).isEqualTo("a\nb");
        assertThat(optionFormatter.describe(new CompileErrorOption())).isEqualTo("Compile error");
        assertThat(optionFormatter.describe(new ExceptionOption("java.lang.IllegalStateException", null)))
                .isEqualTo("Throws java.lang.IllegalStateException");
        assertThat(optionFormatter.describe(new ExceptionOption("java.lang.IllegalStateException", "start")))
                .isEqualTo("start\nThrows java.lang.IllegalStateException");
    }

    @Test
    void questionWithoutCodeShowsNoEmptyCodeBlock() {
        Question question = question(QuestionType.SINGLE_CHOICE, null);

        assertThat(new QuestionFormatter().format(question, new SessionProgress(2, 7))).isEqualTo("""

                Question 3/7 - t
                Pick one

                  1) first
                  2) second""");
    }

    @Test
    void wrongCodeOutputAnswerShowsTheCorrectOptionOnOneLine() {
        Question question = question(QuestionType.CODE_OUTPUT, "int x;").toBuilder()
                .options(List.of(new OutputOption("a\nb"), new ExceptionOption("java.lang.Error", null)))
                .build();
        AnswerResult result = AnswerResult.builder()
                .correct(false)
                .correctAnswer(new SingleIndex(0))
                .explanation("Because.")
                .reference(null)
                .topicId("t")
                .masteryBefore(0.404)
                .masteryAfter(0.0)
                .topicsUnlocked(List.of())
                .build();

        assertThat(new FeedbackFormatter().format(question, result, new Colors(false))).isEqualTo("""
                Wrong.
                Correct answer: 1) a / b
                Because.
                Mastery of t: 40% -> 0%""");
    }

    private static Question question(QuestionType type, String code) {
        return Question.builder()
                .id("t.q")
                .topic("t")
                .type(type)
                .prompt("Pick one")
                .code(code)
                .options(List.of(new TextOption("first"), new TextOption("second")))
                .build();
    }
}
