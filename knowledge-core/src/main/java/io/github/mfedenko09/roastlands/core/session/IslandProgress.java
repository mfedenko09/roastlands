package io.github.mfedenko09.roastlands.core.session;

import io.github.mfedenko09.roastlands.core.topic.Island;
import java.util.List;

public record IslandProgress(Island island, List<TopicProgress> topics) {

    public IslandProgress {
        topics = List.copyOf(topics);
    }
}
