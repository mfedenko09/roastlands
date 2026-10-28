package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.content.Bool;
import io.github.mfedenko09.roastlands.core.content.CorrectAnswer;
import io.github.mfedenko09.roastlands.core.content.IndexSet;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import io.github.mfedenko09.roastlands.core.session.AnswerResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class FeedbackFormatter {

    private static final int PERCENT = 100;

    private final OptionFormatter optionFormatter = new OptionFormatter();

    String format(Question question, AnswerResult result, Colors colors) {
        List<String> lines = new ArrayList<>();
        lines.add(result.correct() ? colors.green("Right!") : colors.red("Wrong."));
        if (!result.correct()) {
            lines.add("Correct answer: " + describe(question, result.correctAnswer()));
        }
        lines.add(result.explanation());
        if (result.reference() != null && !result.reference().isBlank()) {
            lines.add("Reference: " + result.reference());
        }
        lines.add("Mastery of " + result.topicId() + ": " + percent(result.masteryBefore()) + "% -> "
                + percent(result.masteryAfter()) + "%");
        if (!result.topicsUnlocked().isEmpty()) {
            lines.add("Unlocked: " + String.join(", ", result.topicsUnlocked()));
        }
        return String.join("\n", lines);
    }

    static int percent(double mastery) {
        return (int) Math.round(mastery * PERCENT);
    }

    private String describe(Question question, CorrectAnswer correct) {
        return switch (correct) {
            case Bool bool -> bool.value() ? "true" : "false";
            case SingleIndex single -> numbered(question, single.index());
            case IndexSet set -> set.indexes().stream().sorted().map(index -> numbered(question, index))
                    .collect(Collectors.joining("; "));
        };
    }

    private String numbered(Question question, int index) {
        String text = optionFormatter.describe(question.options().get(index)).replace("\n", " / ");
        return (index + 1) + ") " + text;
    }
}
