package io.github.mfedenko09.roastlands.core.session;

import io.github.mfedenko09.roastlands.core.mastery.MasteryLevel;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import java.util.List;
import lombok.Builder;

@Builder
public record TopicProgress(
        Topic topic, MasteryLevel level, double mastery, boolean locked, List<String> weakPrerequisites) {

    public TopicProgress {
        weakPrerequisites = List.copyOf(weakPrerequisites);
    }
}
