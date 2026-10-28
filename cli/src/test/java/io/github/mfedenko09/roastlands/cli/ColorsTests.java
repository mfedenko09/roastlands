package io.github.mfedenko09.roastlands.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ColorsTests {

    @Test
    void colorsNeedAConsoleAndAKnownTerminal() {
        assertThat(Colors.detect(true, Map.of("TERM", "xterm-256color")).enabled()).isTrue();
        assertThat(Colors.detect(true, Map.of("WT_SESSION", "abc")).enabled()).isTrue();
        assertThat(Colors.detect(false, Map.of("TERM", "xterm")).enabled()).isFalse();
        assertThat(Colors.detect(true, Map.of()).enabled()).isFalse();
        assertThat(Colors.detect(true, Map.of("TERM", " ")).enabled()).isFalse();
    }

    @Test
    void enabledColorsWrapTextInAnsiCodes() {
        Colors colors = new Colors(true);

        assertThat(colors.green("Right!")).isEqualTo("\u001B[32mRight!\u001B[0m");
        assertThat(colors.red("Wrong.")).isEqualTo("\u001B[31mWrong.\u001B[0m");
    }

    @Test
    void disabledColorsReturnPlainText() {
        assertThat(new Colors(false).green("Right!")).isEqualTo("Right!");
        assertThat(new Colors(false).red("Wrong.")).isEqualTo("Wrong.");
    }
}
