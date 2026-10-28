package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mfedenko09.roastlands.core.content.InvalidContentException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContentCliTests {

    private static final Path FIXTURES = Path.of("src", "test", "resources", "fixtures");

    @Test
    void verifyPassesOnACorrectPack() {
        assertThatCode(() -> ContentCli.run(new String[] {"verify", FIXTURES.resolve("correct-output").toString()}))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyFailsAndListsEveryBrokenQuestion() {
        assertThatThrownBy(() -> ContentCli.run(new String[] {"verify", FIXTURES.resolve("wrong-key").toString()}))
                .isInstanceOfSatisfying(InvalidContentException.class, e -> assertThat(e.problems()).containsExactly(
                        "pack/questions/core.types.yaml:core.types.wrong-key: "
                                + "marked option is output \"1\", actual outcome is output \"2\""));
    }

    @Test
    void wrongArgumentsPrintTheUsage() {
        assertThatThrownBy(() -> ContentCli.run(new String[] {}))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("usage: verify <packsRoot>");
        assertThatThrownBy(() -> ContentCli.run(new String[] {"check", "x"}))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("usage: verify <packsRoot>");
        assertThatThrownBy(() -> ContentCli.run(new String[] {"verify"}))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("usage: verify <packsRoot>");
    }
}
