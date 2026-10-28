package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mfedenko09.roastlands.core.topic.InvalidTopicMapException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TopicMapCliTests {

    private static final String PACK = "id: p\ntitle: P\nversion: 1.0.0\nminJavaVersion: 21\n";

    @TempDir
    Path packsRoot;

    @Test
    void generateWritesTitleAndMermaidBlock() throws IOException {
        writeTopics("""
                islands:
                  - id: shore
                    title: Shore
                topics:
                  - id: a
                    title: A
                    island: shore
                """);
        Path output = packsRoot.resolveSibling("out").resolve("topic-map.md");

        TopicMapCli.run(new String[] {"generate", packsRoot.toString(), output.toString()});

        assertThat(Files.readString(output)).isEqualTo("""
                # Topic map

                Generated from `content/` by `./gradlew generateTopicMap`. Do not edit by hand.

                ```mermaid
                flowchart TD
                    subgraph shore["Shore"]
                        a["A"]
                    end
                ```
                """);
    }

    @Test
    void verifyFailsOnBrokenMap() throws IOException {
        writeTopics("""
                islands: []
                topics:
                  - id: a
                    title: A
                    island: nowhere
                """);

        assertThatThrownBy(() -> TopicMapCli.run(new String[] {"verify", packsRoot.toString()}))
                .isInstanceOf(InvalidTopicMapException.class)
                .hasMessageContaining("topic a: unknown island \"nowhere\"");
    }

    @Test
    void verifyPassesOnValidMap() throws IOException {
        writeTopics("islands: []\ntopics: []\n");

        TopicMapCli.run(new String[] {"verify", packsRoot.toString()});
    }

    @Test
    void mainRunsTheRequestedCommand() throws IOException {
        writeTopics("islands: []\ntopics: []\n");

        TopicMapCli.main(new String[] {"verify", packsRoot.toString()});
    }

    @Test
    void generateFailsWhenOutputCannotBeWritten() throws IOException {
        writeTopics("islands: []\ntopics: []\n");
        Path directory = Files.createDirectories(packsRoot.resolveSibling("occupied"));

        assertThatThrownBy(() -> TopicMapCli.run(new String[] {"generate", packsRoot.toString(), directory.toString()}))
                .isInstanceOf(UncheckedIOException.class);
    }

    @ParameterizedTest
    @MethodSource("badArguments")
    void unknownCommandOrWrongArgumentCountPrintsUsage(List<String> args) {
        assertThatThrownBy(() -> TopicMapCli.run(args.toArray(String[]::new)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("usage:");
    }

    static Stream<List<String>> badArguments() {
        return Stream.of(List.of("draw"), List.of("draw", "x"), List.of("draw", "x", "y"), List.of("verify"));
    }

    private void writeTopics(String topicsYaml) throws IOException {
        Path packDir = Files.createDirectories(packsRoot.resolve("p"));
        Files.writeString(packDir.resolve("pack.yaml"), PACK);
        Files.writeString(packDir.resolve("topics.yaml"), topicsYaml);
    }
}
