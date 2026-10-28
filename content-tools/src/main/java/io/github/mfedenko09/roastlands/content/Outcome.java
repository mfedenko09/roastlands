package io.github.mfedenko09.roastlands.content;

public sealed interface Outcome permits Output, CompileError, ThrownException {

    String describe();

    static String quote(String text) {
        return "\"" + text.replace("\n", "\\n") + "\"";
    }
}
