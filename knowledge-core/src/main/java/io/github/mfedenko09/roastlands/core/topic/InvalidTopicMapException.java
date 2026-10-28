package io.github.mfedenko09.roastlands.core.topic;

import java.util.List;

public class InvalidTopicMapException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<TopicMapProblem> problems;

    public InvalidTopicMapException(List<TopicMapProblem> problems) {
        super(describe(problems.stream().map(TopicMapProblem::text).toList()));
        this.problems = List.copyOf(problems);
    }

    public InvalidTopicMapException(String message) {
        super(message);
        this.problems = List.of();
    }

    public static String describe(List<String> lines) {
        return "Invalid topic map:" + System.lineSeparator() + String.join(System.lineSeparator(), lines);
    }

    public List<TopicMapProblem> problems() {
        return problems;
    }
}
