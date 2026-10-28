package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.content.Option;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.session.SessionProgress;
import java.util.ArrayList;
import java.util.List;

final class QuestionFormatter {

    private static final String OPTION_LABEL_FORMAT = "  %d) ";
    private static final int OPTION_LABEL_WIDTH = 5;

    private final OptionFormatter optionFormatter = new OptionFormatter();

    String format(Question question, SessionProgress progress) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("Question " + (progress.done() + 1) + "/" + progress.total() + " - " + question.topic());
        lines.add(question.prompt());
        if (question.code() != null && !question.code().isBlank()) {
            lines.add("");
            lines.add(question.code().stripTrailing());
        }
        if (question.type() != QuestionType.TRUE_FALSE) {
            lines.add("");
            lines.addAll(optionLines(question.options()));
        }
        return String.join("\n", lines);
    }

    String promptFor(Question question) {
        int count = question.options().size();
        return switch (question.type()) {
            case CODE_OUTPUT, SINGLE_CHOICE -> "Your answer (1-" + count + "): ";
            case MULTI_CHOICE -> "Your answers, comma separated (1-" + count + "): ";
            case TRUE_FALSE -> "Your answer (t/f): ";
        };
    }

    String hintFor(Question question) {
        int count = question.options().size();
        return switch (question.type()) {
            case CODE_OUTPUT, SINGLE_CHOICE -> "Please type one number from 1 to " + count + ".";
            case MULTI_CHOICE -> "Please type numbers from 1 to " + count + ", each once, like 1,3.";
            case TRUE_FALSE -> "Please type t or f.";
        };
    }

    private List<String> optionLines(List<Option> options) {
        List<String> lines = new ArrayList<>();
        String padding = " ".repeat(OPTION_LABEL_WIDTH);
        for (int i = 0; i < options.size(); i++) {
            String[] parts = optionFormatter.describe(options.get(i)).split("\n", -1);
            lines.add(String.format(OPTION_LABEL_FORMAT, i + 1) + parts[0]);
            for (int part = 1; part < parts.length; part++) {
                lines.add(padding + parts[part]);
            }
        }
        return lines;
    }
}
