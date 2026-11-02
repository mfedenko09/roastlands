package io.github.mfedenko09.roastlands.core.content;

import java.util.Map;

record ParseContext(String fileTopic, Map<String, Integer> keyPointCounts, int packMinJavaVersion, String source) {
}
