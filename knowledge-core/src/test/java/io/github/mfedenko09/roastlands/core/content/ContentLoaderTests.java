package io.github.mfedenko09.roastlands.core.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContentLoaderTests {

    private static final String BASICS_QUESTION = """
            - id: basics.one
              topic: basics
              type: TRUE_FALSE
              difficulty: 1
              prompt: Is it so?
              correct: true
              explanation: Because.
              reference: JLS §1.1
            """;

    private static final String DEEP_QUESTION = BASICS_QUESTION.replace("basics.one", "deep.one")
            .replace("topic: basics", "topic: deep");

    @TempDir
    Path contentDir;

    @Test
    void mergesQuestionsOfAllPacksInFolderOrderAndJoinsPackIds() throws IOException {
        writePack("a-pack", "a-pack", "islands: [{id: shore, title: Shore}]\ntopics:\n"
                + "  - {id: basics, title: Basics, island: shore}\n", BASICS_QUESTION, "basics");
        writePack("b-pack", "b-pack", "topics:\n  - {id: deep, title: Deep, island: shore, prerequisites: [basics]}\n",
                DEEP_QUESTION, "deep");
        Files.createDirectories(contentDir.resolve("packs").resolve("not-a-pack"));

        GameContent content = new ContentLoader().load(contentDir);

        assertThat(content.pack().id()).isEqualTo("a-pack+b-pack");
        assertThat(content.pack().questions()).extracting(Question::id).containsExactly("basics.one", "deep.one");
        assertThat(content.topics().topics()).extracting("id").containsExactly("basics", "deep");
    }

    @Test
    void failsWithAHintWhenThePacksFolderIsMissing() {
        InvalidContentException error = catchThrowableOfType(
                InvalidContentException.class, () -> new ContentLoader().load(contentDir));

        assertThat(error.getMessage()).contains(contentDir.resolve("packs").toAbsolutePath().toString());
    }

    private void writePack(String folder, String id, String topics, String questions, String topicFile)
            throws IOException {
        Path pack = contentDir.resolve("packs").resolve(folder);
        Files.createDirectories(pack.resolve("questions"));
        Files.writeString(pack.resolve("pack.yaml"), "id: " + id + "\ntitle: T\nversion: 1.0.0\nminJavaVersion: 21\n");
        Files.writeString(pack.resolve("topics.yaml"), topics);
        Files.writeString(pack.resolve("questions").resolve(topicFile + ".yaml"), questions);
    }
}
