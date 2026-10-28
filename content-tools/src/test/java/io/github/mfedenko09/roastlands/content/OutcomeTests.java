package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OutcomeTests {

    @Test
    void outputIsDescribedInQuotes() {
        assertThat(new Output("2").describe()).isEqualTo("output \"2\"");
    }

    @Test
    void lineBreaksInOutputAreEscapedSoTheReportKeepsOneLinePerFailure() {
        assertThat(new Output("true\nfalse").describe()).isEqualTo("output \"true\\nfalse\"");
    }

    @Test
    void compileErrorHasAFixedDescription() {
        assertThat(new CompileError().describe()).isEqualTo("compile error");
    }

    @Test
    void exceptionWithoutOutputShowsOnlyTheClass() {
        assertThat(new ThrownException("java.lang.IllegalStateException", "").describe())
                .isEqualTo("exception java.lang.IllegalStateException");
    }

    @Test
    void exceptionWithOutputShowsWhatWasPrintedFirst() {
        assertThat(new ThrownException("java.lang.IllegalStateException", "a\nb").describe())
                .isEqualTo("exception java.lang.IllegalStateException after output \"a\\nb\"");
    }
}
