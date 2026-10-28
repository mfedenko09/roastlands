package io.github.mfedenko09.roastlands.core.topic;

import java.util.List;
import lombok.Builder;

@Builder
public record Topic(
        String id,
        String title,
        String islandId,
        List<String> prerequisites,
        int minJavaVersion,
        List<String> keyPoints) {

    public Topic {
        prerequisites = List.copyOf(prerequisites);
        keyPoints = List.copyOf(keyPoints);
    }
}
