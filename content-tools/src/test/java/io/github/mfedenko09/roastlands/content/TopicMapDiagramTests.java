package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.topic.Island;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

class TopicMapDiagramTests {

    @Test
    void drawsOneSubgraphPerIslandAndOneEdgePerPrerequisite() {
        TopicGraph graph = TopicGraph.of(
                List.of(new Island("core-shore", "Core Shore"), new Island("generics-grotto", "Generics Grotto")),
                List.of(
                        topic("generics.basics", "generics-grotto", "Type \"params\"", "core.oop"),
                        topic("core.oop", "core-shore", "Init order"),
                        topic("core.types", "core-shore", "Primitives")));

        String mermaid = new TopicMapDiagram().toMermaid(graph);

        assertThat(mermaid).isEqualTo("""
                flowchart TD
                    subgraph core_2d_shore["Core Shore"]
                        core_2e_oop["Init order"]
                        core_2e_types["Primitives"]
                    end
                    subgraph generics_2d_grotto["Generics Grotto"]
                        generics_2e_basics["Type #quot;params#quot;"]
                    end
                    core_2e_oop --> generics_2e_basics
                """);
    }

    @Test
    void idsThatDifferOnlyInSeparatorsStayDifferentNodes() {
        TopicGraph graph = TopicGraph.of(
                List.of(new Island("shore", "Shore")),
                List.of(topic("core.a-b", "shore", "Dash"), topic("core.a_b", "shore", "Underscore")));

        String mermaid = new TopicMapDiagram().toMermaid(graph);

        assertThat(mermaid).contains("core_2e_a_2d_b[\"Dash\"]").contains("core_2e_a_5f_b[\"Underscore\"]");
    }

    private static Topic topic(String id, String islandId, String title, String... prerequisites) {
        return Topic.builder()
                .id(id)
                .title(title)
                .islandId(islandId)
                .prerequisites(List.of(prerequisites))
                .minJavaVersion(21)
                .keyPoints(List.of())
                .build();
    }
}
