package io.github.mfedenko09.roastlands.core.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
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

    @Test
    void loadsTheSameContentFromAJarInAFolderWithSpacesAndCyrillicLetters(@TempDir Path temp) throws IOException {
        writeSamplePack();
        Path jar = packIntoJar(Files.createDirectories(temp.resolve("Мої ігри")).resolve("game client.jar"));

        try (URLClassLoader classLoader = classLoaderOf(jar)) {
            GameContent packed = new ContentLoader().loadPacked(classLoader);
            GameContent fromFolder = new ContentLoader().load(contentDir);

            assertThat(packed.pack().questions())
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("source")
                    .isEqualTo(fromFolder.pack().questions());
            assertThat(packed.topics().topics()).isEqualTo(fromFolder.topics().topics());
            assertThat(packed.topics().islands()).isEqualTo(fromFolder.topics().islands());
        }
    }

    @Test
    void loadsPackedContentFromAClasspathFolder(@TempDir Path classes) throws IOException {
        writeSamplePack();
        Path packs = Files.createDirectories(classes.resolve("content/packs"));
        copyTree(contentDir.resolve("packs"), packs);

        try (URLClassLoader classLoader = classLoaderOf(classes)) {
            assertThat(new ContentLoader().loadPacked(classLoader).pack().questions())
                    .extracting(Question::id).containsExactly("basics.one");
        }
    }

    @Test
    void failsWhenTheBuildHasNoPackedContent(@TempDir Path classes) throws IOException {
        try (URLClassLoader classLoader = classLoaderOf(classes)) {
            InvalidContentException error = catchThrowableOfType(
                    InvalidContentException.class, () -> new ContentLoader().loadPacked(classLoader));

            assertThat(error.getMessage()).contains("no packed content");
        }
    }

    private void writeSamplePack() throws IOException {
        writePack("a-pack", "a-pack", "islands: [{id: shore, title: Shore}]\ntopics:\n"
                + "  - {id: basics, title: Basics, island: shore}\n", BASICS_QUESTION, "basics");
    }

    private Path packIntoJar(Path jar) throws IOException {
        try (OutputStream file = Files.newOutputStream(jar);
                JarOutputStream out = new JarOutputStream(file);
                Stream<Path> tree = Files.walk(contentDir)) {
            for (Path path : tree.sorted().toList()) {
                String name = "content/" + contentDir.relativize(path).toString().replace('\\', '/');
                boolean directory = Files.isDirectory(path);
                out.putNextEntry(new JarEntry(directory && !name.endsWith("/") ? name + "/" : name));
                if (!directory) {
                    Files.copy(path, out);
                }
                out.closeEntry();
            }
        }
        return jar;
    }

    private static void copyTree(Path from, Path to) throws IOException {
        List<Path> paths;
        try (Stream<Path> tree = Files.walk(from)) {
            paths = tree.sorted().toList();
        }
        for (Path path : paths) {
            Path target = to.resolve(from.relativize(path).toString());
            if (Files.isDirectory(path)) {
                Files.createDirectories(target);
            } else {
                Files.copy(path, target);
            }
        }
    }

    private static URLClassLoader classLoaderOf(Path location) throws IOException {
        return new URLClassLoader(new URL[] {location.toUri().toURL()}, null);
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
