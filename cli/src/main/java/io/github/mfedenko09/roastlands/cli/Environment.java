package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.profile.ProfilePaths;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import lombok.Builder;

@Builder
record Environment(
        BufferedReader in,
        PrintStream out,
        PrintStream err,
        Clock clock,
        Colors colors,
        Path defaultContentDir,
        Path defaultProfileDir) {

    private static final Path DEFAULT_CONTENT_DIR = Path.of("content");

    static Environment system() {
        return Environment.builder()
                .in(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)))
                .out(System.out)
                .err(System.err)
                .clock(Clock.systemDefaultZone())
                .colors(Colors.detect(System.console() != null, System.getenv()))
                .defaultContentDir(DEFAULT_CONTENT_DIR)
                .defaultProfileDir(ProfilePaths.defaultDir())
                .build();
    }
}
