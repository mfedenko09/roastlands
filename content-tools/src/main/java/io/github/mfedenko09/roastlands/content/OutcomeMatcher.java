package io.github.mfedenko09.roastlands.content;

import io.github.mfedenko09.roastlands.core.content.CompileErrorOption;
import io.github.mfedenko09.roastlands.core.content.ExceptionOption;
import io.github.mfedenko09.roastlands.core.content.Option;
import io.github.mfedenko09.roastlands.core.content.OutputOption;
import io.github.mfedenko09.roastlands.core.content.TextOption;

final class OutcomeMatcher {

    private OutcomeMatcher() {
    }

    static boolean matches(Option option, Outcome actual) {
        return switch (option) {
            case OutputOption expected -> actual instanceof Output output && output.text().equals(expected.output());
            case CompileErrorOption expected -> actual instanceof CompileError;
            case ExceptionOption expected -> actual instanceof ThrownException thrown
                    && thrown.exception().equals(expected.exception())
                    && (!expected.checksOutput() || thrown.output().equals(expected.output()));
            case TextOption text -> throw notAnOutcome(text);
        };
    }

    static String describe(Option option) {
        return switch (option) {
            case OutputOption expected -> new Output(expected.output()).describe();
            case CompileErrorOption expected -> new CompileError().describe();
            case ExceptionOption expected -> expected.checksOutput()
                    ? new ThrownException(expected.exception(), expected.output()).describe()
                    : "exception " + expected.exception();
            case TextOption text -> throw notAnOutcome(text);
        };
    }

    private static IllegalArgumentException notAnOutcome(TextOption option) {
        return new IllegalArgumentException("\"" + option.text() + "\" is not a code outcome");
    }
}
