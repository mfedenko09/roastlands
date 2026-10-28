package io.github.mfedenko09.roastlands.core.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TopicMapLoaderTests {

    private static final String PACK = """
            id: %s
            title: Pack
            version: 1.0.0
            minJavaVersion: 21
            """;

    private static final String ISLANDS = """
            islands:
              - id: shore
                title: Shore
            """;

    private final TopicMapLoader loader = new TopicMapLoader();

    @TempDir
    Path packsRoot;

    @Test
    void loadsTopicsFromEveryPackAndAllowsPrerequisitesAcrossPacks() throws IOException {
        writePack("a", ISLANDS + """
                topics:
                  - id: a.first
                    title: First
                    island: shore
                    keyPoints: [one, two]
                """);
        writePack("b", """
                islands: []
                topics:
                  - id: b.second
                    title: Second
                    island: shore
                    prerequisites: [a.first]
                    minJavaVersion: 17
                """);

        TopicGraph graph = loader.load(packsRoot);

        assertThat(graph.topologicalOrder()).extracting(Topic::id).containsExactly("a.first", "b.second");
        Topic second = graph.topologicalOrder().get(1);
        assertThat(second.prerequisites()).containsExactly("a.first");
        assertThat(second.minJavaVersion()).isEqualTo(17);
    }

    @Test
    void minJavaVersionFallsBackToThePackAndListsDefaultToEmpty() throws IOException {
        writePack("a", ISLANDS + """
                topics:
                  - id: a.first
                    title: First
                    island: shore
                """);

        Topic first = loader.load(packsRoot).topologicalOrder().get(0);

        assertThat(first.minJavaVersion()).isEqualTo(21);
        assertThat(first.prerequisites()).isEmpty();
        assertThat(first.keyPoints()).isEmpty();
    }

    @Test
    void directoryWithoutTopicsFileIsIgnored() throws IOException {
        writePack("a", ISLANDS + "topics: []\n");
        Files.createDirectories(packsRoot.resolve("notes"));

        assertThat(loader.load(packsRoot).topologicalOrder()).isEmpty();
    }

    @Test
    void unknownFieldFailsAndNamesTheFile() throws IOException {
        writePack("a", ISLANDS + """
                topics:
                  - id: a.first
                    title: First
                    island: shore
                    prerequisits: [a.other]
                """);

        assertThatThrownBy(() -> loader.load(packsRoot))
                .isInstanceOf(InvalidTopicMapException.class)
                .hasMessageContaining(topicsFile("a") + ": ")
                .hasMessageContaining("prerequisits");
    }

    @Test
    void unknownPrerequisiteNamesFileAndTopic() throws IOException {
        writePack("a", ISLANDS + """
                topics:
                  - id: a.first
                    title: First
                    island: shore
                    prerequisites: [core.typo]
                """);

        assertThatThrownBy(() -> loader.load(packsRoot))
                .hasMessageContaining(topicsFile("a") + ": topic a.first: unknown prerequisite \"core.typo\"");
    }

    @Test
    void cycleNamesFileAndPath() throws IOException {
        writePack("a", ISLANDS + """
                topics:
                  - id: A
                    title: A
                    island: shore
                    prerequisites: [B]
                  - id: B
                    title: B
                    island: shore
                    prerequisites: [A]
                """);

        assertThatThrownBy(() -> loader.load(packsRoot))
                .hasMessageContaining(topicsFile("a") + ": topic A: dependency cycle A -> B -> A");
    }

    @Test
    void duplicateTopicInTwoPacksNamesBothFiles() throws IOException {
        String pack = ISLANDS + """
                topics:
                  - id: same
                    title: Same
                    island: shore
                """;
        writePack("a", pack);
        writePack("b", pack.replace("shore", "shore2").replace("shore22", "shore2"));

        assertThatThrownBy(() -> loader.load(packsRoot))
                .hasMessageContaining(topicsFile("a") + ", " + topicsFile("b") + ": topic same: duplicate topic id");
    }

    @Test
    void missingPackFileIsReported() throws IOException {
        Path packDir = Files.createDirectories(packsRoot.resolve("a"));
        Files.writeString(packDir.resolve("topics.yaml"), ISLANDS + "topics: []\n");

        assertThatThrownBy(() -> loader.load(packsRoot))
                .hasMessageContaining(packDir.resolve("pack.yaml") + ": cannot be read");
    }

    @Test
    void nullTopicsDocumentIsReported() throws IOException {
        writePack("a", "~\n");

        assertThatThrownBy(() -> loader.load(packsRoot))
                .hasMessageContaining(topicsFile("a") + ": file is empty");
    }

    @Test
    void topicWithoutTitleIsReported() throws IOException {
        writePack("a", ISLANDS + """
                topics:
                  - id: a.first
                    island: shore
                """);

        assertThatThrownBy(() -> loader.load(packsRoot))
                .hasMessageContaining(topicsFile("a") + ": topic entry \"a.first\" needs id, title and island");
    }

    @Test
    void islandWithoutIdIsReportedWithTheFile() throws IOException {
        writePack("a", """
                islands:
                  - title: Nameless
                topics: []
                """);

        assertThatThrownBy(() -> loader.load(packsRoot))
                .hasMessageContaining(topicsFile("a") + ": island entry \"null\" needs id and title");
    }

    @Test
    void packWithoutRequiredFieldsIsReportedWithTheFile() throws IOException {
        Path packDir = Files.createDirectories(packsRoot.resolve("a"));
        Files.writeString(packDir.resolve("pack.yaml"), "id: a\ntitle: A\nversion: 1.0.0\n");
        Files.writeString(packDir.resolve("topics.yaml"), ISLANDS + "topics: []\n");

        assertThatThrownBy(() -> loader.load(packsRoot))
                .hasMessageContaining(packDir.resolve("pack.yaml")
                        + ": pack needs id, title, version and minJavaVersion");
    }

    @Test
    void sameIdInTwoPacksIsReportedWithBothFiles() throws IOException {
        writePack("a", ISLANDS + "topics: []\n");
        Path other = Files.createDirectories(packsRoot.resolve("b"));
        Files.writeString(other.resolve("pack.yaml"), PACK.formatted("a"));
        Files.writeString(other.resolve("topics.yaml"), "islands: []\ntopics: []\n");

        assertThatThrownBy(() -> loader.load(packsRoot))
                .hasMessageContaining(other.resolve("pack.yaml") + ": duplicate pack id \"a\", already used in "
                        + packsRoot.resolve("a").resolve("pack.yaml"));
    }

    @Test
    void missingPacksRootFails() {
        Path missing = packsRoot.resolve("missing");

        assertThatThrownBy(() -> loader.load(missing)).isInstanceOf(UncheckedIOException.class);
    }

    private void writePack(String name, String topicsYaml) throws IOException {
        Path packDir = Files.createDirectories(packsRoot.resolve(name));
        Files.writeString(packDir.resolve("pack.yaml"), PACK.formatted(name));
        Files.writeString(packDir.resolve("topics.yaml"), topicsYaml);
    }

    private String topicsFile(String pack) {
        return packsRoot.resolve(pack).resolve("topics.yaml").toString();
    }
}
