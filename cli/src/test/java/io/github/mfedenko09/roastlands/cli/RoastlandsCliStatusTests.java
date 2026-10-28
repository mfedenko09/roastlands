package io.github.mfedenko09.roastlands.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RoastlandsCliStatusTests {

    @TempDir
    Path content;

    @TempDir
    Path profileDir;

    @BeforeEach
    void writeContent() {
        CliTestSupport.writeContent(content);
    }

    @Test
    void freshProfileShowsNewTopicsAndLocksWithTheirMissingPrerequisites() {
        CliResult result = status();

        assertThat(result.exitCode()).isZero();
        assertThat(result.out()).isEqualTo("""
                Roastlands status for 2026-10-28

                Shore
                  Basics     NEW     0%

                Cave
                  Deep Dive  NEW     0%  locked, needs: basics

                Today 0/4
                """);
    }

    @Test
    void afterASessionTheTopicIsSharpTheNextOneIsOpenAndTodayShowsProgress() {
        CliTestSupport.run("t\ny\n2\ny\n1,3\ny\n1\ny\n", new Colors(false), content, profileDir, "review");

        assertThat(status().out()).isEqualTo("""
                Roastlands status for 2026-10-28

                Shore
                  Basics     SHARP 100%

                Cave
                  Deep Dive  NEW     0%

                Today 4/5
                """);
    }

    @Test
    void oneAnsweredQuestionOfFourLeavesTheTopicRusty() {
        CliTestSupport.run("f\ny\n", new Colors(false), content, profileDir, "review");

        assertThat(status().out()).contains("  Basics     RUSTY  25%\n").contains("Today 1/4\n");
    }

    private CliResult status() {
        return CliTestSupport.run("", new Colors(false), content, profileDir, "status");
    }
}
