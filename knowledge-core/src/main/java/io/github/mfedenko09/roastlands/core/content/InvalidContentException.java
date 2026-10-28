package io.github.mfedenko09.roastlands.core.content;

import java.util.List;

public class InvalidContentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> problems;

    public InvalidContentException(List<String> problems) {
        super("Invalid content:" + System.lineSeparator() + String.join(System.lineSeparator(), problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
