package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.content.CompileErrorOption;
import io.github.mfedenko09.roastlands.core.content.ExceptionOption;
import io.github.mfedenko09.roastlands.core.content.Option;
import io.github.mfedenko09.roastlands.core.content.OutputOption;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.TextOption;
import java.util.List;

final class OptionLabels {

    static final String TRUE_LABEL = "True";
    static final String FALSE_LABEL = "False";

    private static final String PRINTS = "Prints: ";
    private static final String DOES_NOT_COMPILE = "Does not compile";
    private static final String THROWS = "Throws ";
    private static final String THEN_THROWS = ", then throws ";
    private static final String LINE_SEPARATOR = " / ";

    private OptionLabels() {
    }

    static List<String> forQuestion(Question question) {
        if (question.type() == QuestionType.TRUE_FALSE) {
            return List.of(TRUE_LABEL, FALSE_LABEL);
        }
        return question.options().stream().map(OptionLabels::label).toList();
    }

    static String label(Option option) {
        return switch (option) {
            case TextOption text -> text.text();
            case OutputOption output -> PRINTS + oneLine(output.output());
            case CompileErrorOption ignored -> DOES_NOT_COMPILE;
            case ExceptionOption exception -> label(exception);
        };
    }

    private static String label(ExceptionOption option) {
        if (!option.checksOutput()) {
            return THROWS + option.exception();
        }
        return PRINTS + oneLine(option.output()) + THEN_THROWS + option.exception();
    }

    private static String oneLine(String output) {
        return output.strip().replace("\n", LINE_SEPARATOR);
    }
}
