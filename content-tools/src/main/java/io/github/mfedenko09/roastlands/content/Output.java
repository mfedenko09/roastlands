package io.github.mfedenko09.roastlands.content;

public record Output(String text) implements Outcome {

    @Override
    public String describe() {
        return "output " + Outcome.quote(text);
    }
}
