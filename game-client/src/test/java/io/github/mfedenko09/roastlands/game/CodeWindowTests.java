package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodeWindowTests {

    private static final String FIVE_LINES = "l1\nl2\nl3\nl4\nl5\n";

    @Test
    void shortCodeIsShownWholeAndCannotScroll() {
        CodeWindow window = new CodeWindow("a\n\tb\n", 3);

        assertThat(window.visibleLines()).containsExactly("a", "    b");
        assertThat(window.isScrollable()).isFalse();
        assertThat(window.totalLines()).isEqualTo(2);
    }

    @Test
    void missingCodeHasNoLines() {
        assertThat(new CodeWindow(null, 3).totalLines()).isZero();
    }

    @Test
    void scrollMovesTheWindowAndStopsAtBothEnds() {
        CodeWindow window = new CodeWindow(FIVE_LINES, 3);

        window.scroll(-1);
        assertThat(window.visibleLines()).containsExactly("l1", "l2", "l3");
        window.scroll(1);
        assertThat(window.visibleLines()).containsExactly("l2", "l3", "l4");
        window.scroll(10);
        assertThat(window.visibleLines()).containsExactly("l3", "l4", "l5");
        assertThat(window.firstLineNumber()).isEqualTo(3);
        assertThat(window.lastLineNumber()).isEqualTo(5);
        assertThat(window.isScrollable()).isTrue();
    }

    @Test
    void scrollingShortCodeChangesNothing() {
        CodeWindow window = new CodeWindow("a", 3);

        window.scroll(2);

        assertThat(window.visibleLines()).containsExactly("a");
    }
}
