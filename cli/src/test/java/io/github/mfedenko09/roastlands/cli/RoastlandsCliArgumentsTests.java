package io.github.mfedenko09.roastlands.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RoastlandsCliArgumentsTests {

    private static final String USAGE = "usage: roastlands-cli <review|status> [--content <dir>] [--profile <dir>]\n";

    @TempDir
    Path content;

    @TempDir
    Path profileDir;

    @TempDir
    Path elsewhere;

    @Test
    void missingCommandPrintsUsage() {
        CliResult result = run();

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.out()).isEmpty();
        assertThat(result.err()).isEqualTo("roastlands-cli: command is missing\n" + USAGE);
    }

    @Test
    void unknownCommandPrintsUsage() {
        CliResult result = run("play");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).isEqualTo("roastlands-cli: unknown command play\n" + USAGE);
    }

    @Test
    void unknownOptionPrintsUsage() {
        assertThat(run("status", "--color", "no").err()).startsWith("roastlands-cli: unknown option --color\n");
    }

    @Test
    void optionWithoutValuePrintsUsage() {
        assertThat(run("status", "--content").err())
                .startsWith("roastlands-cli: option --content needs a value\n");
    }

    @Test
    void valueWithoutOptionNamePrintsUsage() {
        assertThat(run("status", "content").err()).startsWith("roastlands-cli: unexpected argument content\n");
    }

    @Test
    void commandNameIsCaseInsensitive() {
        CliTestSupport.writeContent(content);

        assertThat(run("STATUS").exitCode()).isZero();
    }

    @Test
    void optionsOverrideTheDefaultFolders() {
        CliTestSupport.writeContent(elsewhere.resolve("my-content"));
        Path otherProfile = elsewhere.resolve("my-profile");

        CliResult result = run("review", "--content", elsewhere.resolve("my-content").toString(),
                "--profile", otherProfile.toString());

        assertThat(result.exitCode()).isZero();
        assertThat(result.out()).contains("Question 1/4");
        assertThat(Files.exists(profileDir.resolve("profile.json"))).isFalse();
    }

    @Test
    void missingContentFolderFailsWithAMessage() {
        CliResult result = run("review");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).contains("Cannot find ").contains("--content <dir>");
    }

    @Test
    void invalidContentFailsWithTheProblemList() {
        CliTestSupport.writeContent(content);
        CliTestSupport.write(content.resolve("packs/demo/questions/basics.yaml"), "- id: x\n");

        CliResult result = run("review");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).startsWith("roastlands-cli: Invalid content:");
    }

    @Test
    void corruptedProfileFailsWithoutChangingIt() {
        CliTestSupport.writeContent(content);
        CliTestSupport.write(profileDir.resolve("profile.json"), "{ broken");
        CliTestSupport.write(profileDir.resolve("profile.json.bak"), "{ also broken");

        CliResult result = run("review");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.err()).startsWith("roastlands-cli: Profile is corrupted:");
    }

    private CliResult run(String... args) {
        return CliTestSupport.run("", new Colors(false), content, profileDir, args);
    }
}
