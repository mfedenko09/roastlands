package io.github.mfedenko09.roastlands.core.topic;

import java.util.List;

record TopicEntry(
        String id,
        String title,
        String island,
        List<String> prerequisites,
        Integer minJavaVersion,
        List<String> keyPoints) {
}
