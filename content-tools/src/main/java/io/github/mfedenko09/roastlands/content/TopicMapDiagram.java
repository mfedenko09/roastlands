package io.github.mfedenko09.roastlands.content;

import io.github.mfedenko09.roastlands.core.topic.Island;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.util.List;

public final class TopicMapDiagram {

    private static final String INDENT = "    ";

    public String toMermaid(TopicGraph graph) {
        StringBuilder mermaid = new StringBuilder("flowchart TD\n");
        List<Topic> topics = graph.topologicalOrder();
        for (Island island : graph.islands()) {
            mermaid.append(INDENT).append("subgraph ").append(nodeId(island.id()))
                    .append("[\"").append(label(island.title())).append("\"]\n");
            topics.stream()
                    .filter(topic -> topic.islandId().equals(island.id()))
                    .forEach(topic -> mermaid.append(INDENT).append(INDENT).append(nodeId(topic.id()))
                            .append("[\"").append(label(topic.title())).append("\"]\n"));
            mermaid.append(INDENT).append("end\n");
        }
        for (Topic topic : topics) {
            for (String prerequisite : topic.prerequisites()) {
                mermaid.append(INDENT).append(nodeId(prerequisite)).append(" --> ")
                        .append(nodeId(topic.id())).append('\n');
            }
        }
        return mermaid.toString();
    }

    private static String nodeId(String id) {
        StringBuilder nodeId = new StringBuilder();
        id.codePoints().forEach(codePoint -> {
            if (isPlainIdCharacter(codePoint)) {
                nodeId.appendCodePoint(codePoint);
            } else {
                nodeId.append('_').append(Integer.toHexString(codePoint)).append('_');
            }
        });
        return nodeId.toString();
    }

    private static boolean isPlainIdCharacter(int codePoint) {
        return codePoint < 128 && Character.isLetterOrDigit(codePoint);
    }

    private static String label(String title) {
        return title.replace("\"", "#quot;");
    }
}
