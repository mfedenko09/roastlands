package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.content.CompileErrorOption;
import io.github.mfedenko09.roastlands.core.content.ExceptionOption;
import io.github.mfedenko09.roastlands.core.content.Option;
import io.github.mfedenko09.roastlands.core.content.OutputOption;
import io.github.mfedenko09.roastlands.core.content.TextOption;

final class OptionFormatter {

    private static final String COMPILE_ERROR = "Compile error";
    private static final String THROWS = "Throws ";
    private static final String THEN_PRINTS_SEPARATOR = "\n";

    String describe(Option option) {
        return switch (option) {
            case TextOption text -> text.text();
            case OutputOption output -> output.output();
            case CompileErrorOption ignored -> COMPILE_ERROR;
            case ExceptionOption exception -> describe(exception);
        };
    }

    private static String describe(ExceptionOption option) {
        String thrown = THROWS + option.exception();
        return option.checksOutput() ? option.output() + THEN_PRINTS_SEPARATOR + thrown : thrown;
    }
}
