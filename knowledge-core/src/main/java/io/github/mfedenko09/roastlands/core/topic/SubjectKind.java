package io.github.mfedenko09.roastlands.core.topic;

public enum SubjectKind {
    TOPIC("topic"),
    ISLAND("island");

    private final String label;

    SubjectKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
