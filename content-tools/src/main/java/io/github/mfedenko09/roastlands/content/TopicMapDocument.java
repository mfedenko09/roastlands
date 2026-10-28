package io.github.mfedenko09.roastlands.content;

import io.github.mfedenko09.roastlands.core.topic.TopicGraph;

public final class TopicMapDocument {

    private TopicMapDocument() {
    }

    public static String render(TopicGraph graph) {
        return "# Topic map\n\n"
                + "Generated from `content/` by `./gradlew generateTopicMap`. Do not edit by hand.\n\n"
                + "```mermaid\n" + new TopicMapDiagram().toMermaid(graph) + "```\n";
    }
}
