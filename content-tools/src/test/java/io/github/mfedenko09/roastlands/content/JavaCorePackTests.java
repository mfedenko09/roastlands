package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import io.github.mfedenko09.roastlands.core.topic.TopicMapLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JavaCorePackTests {

    private static final Path PACKS_ROOT = Path.of("..", "content", "packs");

    private static final Path TOPIC_MAP_DOC = Path.of("..", "docs", "topic-map.md");

    private final TopicGraph graph = new TopicMapLoader().load(PACKS_ROOT);

    @Test
    void realMapHas37TopicsOn7Islands() {
        assertThat(graph.topics()).hasSize(37);
        assertThat(graph.islands()).hasSize(7);
    }

    @Test
    void onlyCoreTypesAndCoreOopAreOpenAtTheStart() {
        assertThat(graph.unlockable(Map.of(), Set.of())).containsExactlyInAnyOrder("core.types", "core.oop");
    }

    @Test
    void everyTopicHasThreeToSixKeyPointsAndPackJavaVersion() {
        assertThat(graph.topics()).allSatisfy(topic -> {
            assertThat(topic.keyPoints()).as(topic.id()).hasSizeBetween(3, 6);
            assertThat(topic.minJavaVersion()).as(topic.id()).isEqualTo(21);
        });
    }

    @Test
    void topicMapDocumentIsUpToDateWithTheYaml() throws IOException {
        String committed = Files.readString(TOPIC_MAP_DOC);

        assertThat(committed).as("run ./gradlew generateTopicMap").isEqualTo(TopicMapDocument.render(graph));
    }

    @Test
    void parallelStreamsNeedAnExecutorFromAnotherIsland() {
        Topic parallel = graph.topics().stream()
                .filter(topic -> topic.id().equals("functional.parallel-streams"))
                .findFirst().orElseThrow();

        assertThat(parallel.prerequisites()).containsExactly("functional.collectors", "conc.executors");
    }
}
