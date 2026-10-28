package io.github.mfedenko09.roastlands.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnswerParserTests {

    private final AnswerParser parser = new AnswerParser();

    @Test
    void singleChoiceTurnsTheShownNumberIntoAZeroBasedIndex() {
        assertThat(parser.parseSingle("3", 4)).contains(Set.of(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "5", "-1", "1,2", "a", "", "  ", "01", "1.5"})
    void singleChoiceRejectsAnythingButOneNumberInRange(String input) {
        assertThat(parser.parseSingle(input, 4)).isEmpty();
    }

    @Test
    void multipleChoiceAcceptsCommasAndSpaces() {
        assertThat(parser.parseMulti("1, 3 4", 4)).contains(Set.of(0, 2, 3));
        assertThat(parser.parseMulti("2", 4)).contains(Set.of(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1,1", "1,5", ",1", "1,x", "", "0"})
    void multipleChoiceRejectsRepeatsOutOfRangeAndJunk(String input) {
        assertThat(parser.parseMulti(input, 4)).isEmpty();
    }

    @Test
    void trueFalseAcceptsLettersAndWordsInAnyCase() {
        assertThat(parser.parseTrueFalse("t")).contains(true);
        assertThat(parser.parseTrueFalse("TRUE")).contains(true);
        assertThat(parser.parseTrueFalse("f")).contains(false);
        assertThat(parser.parseTrueFalse("False")).contains(false);
        assertThat(parser.parseTrueFalse("yes")).isEqualTo(Optional.empty());
    }

    @Test
    void sureAcceptsYesAndNoOnly() {
        assertThat(parser.parseSure("y")).contains(true);
        assertThat(parser.parseSure("YES")).contains(true);
        assertThat(parser.parseSure("n")).contains(false);
        assertThat(parser.parseSure("No")).contains(false);
        assertThat(parser.parseSure("t")).isEmpty();
        assertThat(parser.parseSure("")).isEmpty();
    }
}
