package io.github.mfedenko09.roastlands.core.topic;

public record TopicMapSubject(SubjectKind kind, String id) {

    public static TopicMapSubject topic(String id) {
        return new TopicMapSubject(SubjectKind.TOPIC, id);
    }

    public static TopicMapSubject island(String id) {
        return new TopicMapSubject(SubjectKind.ISLAND, id);
    }

    public String text() {
        return kind.label() + " " + id;
    }
}
