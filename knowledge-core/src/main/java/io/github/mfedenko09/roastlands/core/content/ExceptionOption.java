package io.github.mfedenko09.roastlands.core.content;

public record ExceptionOption(String exception, String output) implements Option {

    public boolean checksOutput() {
        return output != null;
    }
}
