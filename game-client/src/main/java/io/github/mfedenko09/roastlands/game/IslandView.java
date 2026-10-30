package io.github.mfedenko09.roastlands.game;

import java.util.List;

record IslandView(String title, String summary, List<TopicView> topics) {

    IslandView {
        topics = List.copyOf(topics);
    }

    boolean hasOpenTopics() {
        return topics.stream().anyMatch(topic -> !topic.locked());
    }
}
