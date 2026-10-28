package io.github.mfedenko09.roastlands.core.topic;

public record TopicMapProblem(TopicMapSubject subject, String description) {

    public String text() {
        return subject.text() + ": " + description;
    }
}
