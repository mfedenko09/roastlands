package io.github.mfedenko09.roastlands.core.content;

import java.util.Set;

record ParseContext(String fileTopic, Set<String> knownTopics, int packMinJavaVersion, String source) {
}
