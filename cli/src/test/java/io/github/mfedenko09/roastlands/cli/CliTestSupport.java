package io.github.mfedenko09.roastlands.cli;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

final class CliTestSupport {

    static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");

    private static final String PACK = "id: demo\ntitle: Demo\nversion: 1.0.0\nminJavaVersion: 21\n";

    private static final String TOPICS = """
            islands:
              - id: shore
                title: Shore
              - id: cave
                title: Cave
            topics:
              - id: basics
                title: Basics
                island: shore
              - id: deep
                title: Deep Dive
                island: cave
                prerequisites: [basics]
            """;

    private static final String BASICS = """
            - id: basics.true-false
              topic: basics
              type: TRUE_FALSE
              difficulty: 1
              prompt: True or false - Java is pass-by-value.
              correct: true
              explanation: Java copies the value of the argument, also for references.
              reference: JLS §15.12.4.5
            - id: basics.single
              topic: basics
              type: SINGLE_CHOICE
              difficulty: 2
              prompt: Which keyword stops inheritance?
              options:
                - abstract
                - final
                - static
              correct: 1
              explanation: A final class cannot be extended.
              reference: JLS §8.1.1.2
            - id: basics.multi
              topic: basics
              type: MULTI_CHOICE
              difficulty: 3
              prompt: Which are primitive types?
              options:
                - int
                - String
                - boolean
                - Integer
              correct: [0, 2]
              explanation: String and Integer are classes.
              reference: JLS §4.2
            - id: basics.output
              topic: basics
              type: CODE_OUTPUT
              difficulty: 3
              prompt: What does this code print?
              code: |
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(1 + 2 + "3");
                        System.out.println("1" + 2 + 3);
                    }
                }
              options:
                - output: "33\\n123"
                - output: "123\\n123"
                - compileError: true
              correct: 0
              explanation: Plus is evaluated from left to right.
              reference: JLS §15.18
            """;

    private static final String DEEP = """
            - id: deep.one
              topic: deep
              type: TRUE_FALSE
              difficulty: 1
              prompt: True or false - a deep question.
              correct: false
              explanation: Because.
              reference: JLS §1
            """;

    private CliTestSupport() {
    }

    static Path writeContent(Path contentDir) {
        Path pack = contentDir.resolve("packs").resolve("demo");
        write(pack.resolve("pack.yaml"), PACK);
        write(pack.resolve("topics.yaml"), TOPICS);
        write(pack.resolve("questions").resolve("basics.yaml"), BASICS);
        write(pack.resolve("questions").resolve("deep.yaml"), DEEP);
        return contentDir;
    }

    static void write(Path file, String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static CliResult run(String input, Colors colors, Path defaultContent, Path defaultProfile, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Environment environment = Environment.builder()
                .in(new BufferedReader(new StringReader(input)))
                .out(new PrintStream(out, true, StandardCharsets.UTF_8))
                .err(new PrintStream(err, true, StandardCharsets.UTF_8))
                .clock(Clock.fixed(NOW, ZoneOffset.UTC))
                .colors(colors)
                .defaultContentDir(defaultContent)
                .defaultProfileDir(defaultProfile)
                .build();
        int exitCode = RoastlandsCli.run(args, environment);
        return new CliResult(exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }
}
