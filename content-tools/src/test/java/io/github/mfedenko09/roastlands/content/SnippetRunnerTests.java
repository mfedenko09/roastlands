package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SnippetRunnerTests {

    private static final int JAVA = 21;

    private final SnippetRunner runner = new SnippetRunner();

    @Test
    void defaultTimeoutIsTenSeconds() {
        assertThat(SnippetRunner.DEFAULT_TIMEOUT).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void returnsStdoutAsOutput() {
        assertThat(runner.run(main("System.out.println(\"true\"); System.out.println(\"false\");"), JAVA))
                .isEqualTo(new Output("true\nfalse"));
    }

    @Test
    void normalizesLineEndsAndRemovesExactlyOneTrailingNewline() {
        assertThat(runner.run(main("System.out.print(\"a\\r\\nb\\r\\n\\r\\n\");"), JAVA))
                .isEqualTo(new Output("a\nb\n"));
    }

    @Test
    void programThatPrintsNothingHasEmptyOutput() {
        assertThat(runner.run(main(""), JAVA)).isEqualTo(new Output(""));
    }

    @Test
    void nonAsciiOutputSurvives() {
        assertThat(runner.run(main("System.out.println(\"héllo ✓\");"), JAVA)).isEqualTo(new Output("héllo ✓"));
    }

    @Test
    void stdinIsClosed() {
        assertThat(runner.run(main("System.out.println(System.in.read());").replace("args) {",
                "args) throws Exception {"), JAVA)).isEqualTo(new Output("-1"));
    }

    @Test
    void codeThatDoesNotCompileIsACompileError() {
        assertThat(runner.run(main("int value = 1"), JAVA)).isEqualTo(new CompileError());
    }

    @Test
    void releaseIsHonoured() {
        String code = main("var value = 1; System.out.println(value);");

        assertThat(runner.run(code, JAVA)).isEqualTo(new Output("1"));
        assertThat(runner.run(code, 8)).isEqualTo(new CompileError());
    }

    @Test
    void uncaughtExceptionIsReportedWithTheOutputPrintedBefore() {
        assertThat(runner.run(main("System.out.println(\"before\"); throw new IllegalStateException(\"boom\");"), JAVA))
                .isEqualTo(new ThrownException("java.lang.IllegalStateException", "before"));
    }

    @Test
    void nestedExceptionClassUsesTheBinaryName() {
        String code = """
                public class Main {
                    static class Boom extends RuntimeException {
                    }

                    public static void main(String[] args) {
                        throw new Boom();
                    }
                }
                """;

        assertThat(runner.run(code, JAVA)).isEqualTo(new ThrownException("Main$Boom", ""));
    }

    @Test
    void nonZeroExitWithoutAnExceptionIsACrash() {
        assertThatThrownBy(() -> runner.run(main("System.exit(3);"), JAVA))
                .isInstanceOf(SnippetRunException.class)
                .hasMessage("exit code 3 without an uncaught exception in main");
    }

    @Test
    void outputThatChangesBetweenRunsIsNotStable() {
        assertThatThrownBy(() -> runner.run(main("System.out.println(System.nanoTime());"), JAVA))
                .isInstanceOf(SnippetRunException.class)
                .hasMessage("outcome is not stable between runs");
    }

    @Test
    void endlessLoopTimesOutAndNoJavaProcessIsLeftRunning() {
        SnippetRunner quick = new SnippetRunner(Duration.ofSeconds(1));

        assertThatThrownBy(() -> quick.run(main("while (true) { }"), JAVA))
                .isInstanceOf(SnippetRunException.class)
                .hasMessage("timeout after 1 s");

        assertThat(snippetProcessesStillAlive()).isEmpty();
    }

    @Test
    void endlessStdoutIsCutAtTheLimitAndTheProcessIsKilled() {
        SnippetRunner small = new SnippetRunner(Duration.ofSeconds(10), 1024);

        assertThatThrownBy(() -> small.run(main("while (true) { System.out.println(\"spam spam spam\"); }"), JAVA))
                .isInstanceOf(SnippetRunException.class)
                .hasMessage("output exceeds 1024 bytes");

        assertThat(snippetProcessesStillAlive()).isEmpty();
    }

    @Test
    void endlessStderrIsCutAtTheLimitToo() {
        SnippetRunner small = new SnippetRunner(Duration.ofSeconds(10), 1024);

        assertThatThrownBy(() -> small.run(main("while (true) { System.err.println(\"spam spam spam\"); }"), JAVA))
                .hasMessage("output exceeds 1024 bytes");
    }

    @Test
    void outputExactlyAtTheLimitIsAccepted() {
        SnippetRunner small = new SnippetRunner(Duration.ofSeconds(10), 4);

        assertThat(small.run(main("System.out.print(\"abcd\");"), JAVA)).isEqualTo(new Output("abcd"));
    }

    @Test
    void defaultOutputLimitIsOneMegabyteAndReportedInMegabytes() {
        assertThat(SnippetRunner.DEFAULT_MAX_OUTPUT_BYTES).isEqualTo(1024 * 1024);
        assertThat(SnippetRunException.outputTooLarge(SnippetRunner.DEFAULT_MAX_OUTPUT_BYTES))
                .hasMessage("output exceeds 1 MB");
    }

    @Test
    void subSecondTimeoutIsReportedInMilliseconds() {
        SnippetRunner quick = new SnippetRunner(Duration.ofMillis(500));

        assertThatThrownBy(() -> quick.run(main("while (true) { }"), JAVA))
                .hasMessage("timeout after 500 ms");
    }

    @Test
    void compilesTellsWhetherTheCodeCompiles() {
        assertThat(runner.compiles(main("System.out.println(1);"), JAVA)).isTrue();
        assertThat(runner.compiles(main("System.out.println(1)"), JAVA)).isFalse();
    }

    @Test
    void temporaryDirectoriesAreDeletedAfterEveryOutcome() throws IOException {
        Set<String> before = snippetDirectories();

        runner.run(main("System.out.println(1);"), JAVA);
        runner.run(main("int value = 1"), JAVA);
        runner.compiles(main(""), JAVA);
        assertThatThrownBy(() -> runner.run(main("System.out.println(System.nanoTime());"), JAVA))
                .isInstanceOf(SnippetRunException.class);

        assertThat(snippetDirectories()).isEqualTo(before);
    }

    private static String main(String body) {
        return "public class Main {\n    public static void main(String[] args) {\n        " + body + "\n    }\n}\n";
    }

    private static Set<String> snippetDirectories() throws IOException {
        try (Stream<Path> children = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return children.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith(SnippetRunner.TEMP_DIR_PREFIX))
                    .collect(Collectors.toSet());
        }
    }

    private static Set<Long> snippetProcessesStillAlive() {
        return ProcessHandle.allProcesses()
                .filter(handle -> handle.info().arguments()
                        .map(arguments -> Arrays.stream(arguments).anyMatch(
                                argument -> argument.contains(SnippetRunner.TEMP_DIR_PREFIX)))
                        .orElse(false))
                .map(ProcessHandle::pid)
                .collect(Collectors.toSet());
    }
}
