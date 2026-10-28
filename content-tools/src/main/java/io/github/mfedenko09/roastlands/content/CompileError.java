package io.github.mfedenko09.roastlands.content;

public record CompileError() implements Outcome {

    @Override
    public String describe() {
        return "compile error";
    }
}
