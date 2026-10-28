package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mfedenko09.roastlands.core.content.CompileErrorOption;
import io.github.mfedenko09.roastlands.core.content.ExceptionOption;
import io.github.mfedenko09.roastlands.core.content.OutputOption;
import io.github.mfedenko09.roastlands.core.content.TextOption;
import org.junit.jupiter.api.Test;

class OutcomeMatcherTests {

    private static final String ISE = "java.lang.IllegalStateException";

    @Test
    void outputOptionMatchesOnlyTheSameText() {
        assertThat(OutcomeMatcher.matches(new OutputOption("a"), new Output("a"))).isTrue();
        assertThat(OutcomeMatcher.matches(new OutputOption("a"), new Output("b"))).isFalse();
        assertThat(OutcomeMatcher.matches(new OutputOption("a"), new CompileError())).isFalse();
        assertThat(OutcomeMatcher.matches(new OutputOption("a"), new ThrownException(ISE, "a"))).isFalse();
    }

    @Test
    void compileErrorOptionMatchesOnlyACompileError() {
        assertThat(OutcomeMatcher.matches(new CompileErrorOption(), new CompileError())).isTrue();
        assertThat(OutcomeMatcher.matches(new CompileErrorOption(), new Output(""))).isFalse();
    }

    @Test
    void exceptionOptionWithoutOutputIgnoresWhatWasPrinted() {
        ExceptionOption option = new ExceptionOption(ISE, null);

        assertThat(OutcomeMatcher.matches(option, new ThrownException(ISE, ""))).isTrue();
        assertThat(OutcomeMatcher.matches(option, new ThrownException(ISE, "printed"))).isTrue();
        assertThat(OutcomeMatcher.matches(option, new ThrownException("java.lang.Error", ""))).isFalse();
        assertThat(OutcomeMatcher.matches(option, new Output(""))).isFalse();
    }

    @Test
    void exceptionOptionWithOutputNeedsTheSameOutput() {
        ExceptionOption option = new ExceptionOption(ISE, "start");

        assertThat(OutcomeMatcher.matches(option, new ThrownException(ISE, "start"))).isTrue();
        assertThat(OutcomeMatcher.matches(option, new ThrownException(ISE, ""))).isFalse();
        assertThat(OutcomeMatcher.matches(option, new ThrownException(ISE, "other"))).isFalse();
    }

    @Test
    void optionsAreDescribedLikeOutcomes() {
        assertThat(OutcomeMatcher.describe(new OutputOption("1"))).isEqualTo("output \"1\"");
        assertThat(OutcomeMatcher.describe(new CompileErrorOption())).isEqualTo("compile error");
        assertThat(OutcomeMatcher.describe(new ExceptionOption(ISE, null))).isEqualTo("exception " + ISE);
        assertThat(OutcomeMatcher.describe(new ExceptionOption(ISE, "start")))
                .isEqualTo("exception " + ISE + " after output \"start\"");
    }

    @Test
    void textOptionIsNotACodeOutcome() {
        assertThatThrownBy(() -> OutcomeMatcher.matches(new TextOption("yes"), new Output("yes")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("\"yes\" is not a code outcome");
        assertThatThrownBy(() -> OutcomeMatcher.describe(new TextOption("yes")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("\"yes\" is not a code outcome");
    }
}
