package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mfedenko09.roastlands.core.content.InvalidContentException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackedContentCheckTests {

    private static final Path REPO_CONTENT = Path.of("../content");

    @Test
    void passesWhenPackedContentIsTheSameAsTheSource() {
        assertThatCode(() -> PackedContentCheck.verify(REPO_CONTENT)).doesNotThrowAnyException();
    }

    @Test
    void failsWhenPackedContentDiffersFromTheSource(@TempDir Path source) throws IOException {
        Path pack = Files.createDirectories(source.resolve("packs/only"));
        Files.writeString(pack.resolve("pack.yaml"), "id: only\ntitle: T\nversion: 1.0.0\nminJavaVersion: 21\n");
        Files.writeString(pack.resolve("topics.yaml"), "islands: [{id: s, title: S}]\ntopics: []\n");

        assertThatThrownBy(() -> PackedContentCheck.verify(source))
                .isInstanceOf(InvalidContentException.class)
                .hasMessageContaining("Packed content differs");
    }
}
