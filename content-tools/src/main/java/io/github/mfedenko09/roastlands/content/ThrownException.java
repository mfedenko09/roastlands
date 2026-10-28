package io.github.mfedenko09.roastlands.content;

public record ThrownException(String exception, String output) implements Outcome {

    @Override
    public String describe() {
        String described = "exception " + exception;
        return output.isEmpty() ? described : described + " after output " + Outcome.quote(output);
    }
}
