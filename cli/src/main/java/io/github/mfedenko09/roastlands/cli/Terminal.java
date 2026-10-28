package io.github.mfedenko09.roastlands.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Function;

final class Terminal {

    private final BufferedReader in;
    private final PrintStream out;
    private final Colors colors;

    Terminal(BufferedReader in, PrintStream out, Colors colors) {
        this.in = in;
        this.out = out;
        this.colors = colors;
    }

    Colors colors() {
        return colors;
    }

    void println(String text) {
        out.println(text);
    }

    <T> Optional<T> ask(String prompt, Function<String, Optional<T>> parser, String hint) {
        while (true) {
            out.print(prompt);
            out.flush();
            String line = readLine();
            if (line == null) {
                return Optional.empty();
            }
            Optional<T> parsed = parser.apply(line.trim());
            if (parsed.isPresent()) {
                return parsed;
            }
            out.println(hint);
        }
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read from the terminal", e);
        }
    }
}
