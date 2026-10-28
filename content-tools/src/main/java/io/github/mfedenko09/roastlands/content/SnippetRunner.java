package io.github.mfedenko09.roastlands.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class SnippetRunner {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    public static final int DEFAULT_MAX_OUTPUT_BYTES = 1024 * 1024;

    static final int RUNS = 3;
    static final String TEMP_DIR_PREFIX = "roastlands-snippet-";

    private static final String MAIN_CLASS = "Main";
    private static final Pattern UNCAUGHT_EXCEPTION = Pattern.compile(
            "^Exception in thread \"main\" ([\\w.$]+)", Pattern.MULTILINE);

    private final Duration timeout;
    private final int maxOutputBytes;

    public SnippetRunner() {
        this(DEFAULT_TIMEOUT, DEFAULT_MAX_OUTPUT_BYTES);
    }

    public SnippetRunner(Duration timeout) {
        this(timeout, DEFAULT_MAX_OUTPUT_BYTES);
    }

    public SnippetRunner(Duration timeout, int maxOutputBytes) {
        this.timeout = timeout;
        this.maxOutputBytes = maxOutputBytes;
    }

    public boolean compiles(String code, int release) {
        Path dir = createTempDir();
        try {
            return compile(dir, code, release);
        } finally {
            deleteRecursively(dir);
        }
    }

    public Outcome run(String code, int release) {
        Path dir = createTempDir();
        try {
            if (!compile(dir, code, release)) {
                return new CompileError();
            }
            Outcome first = runOnce(dir);
            for (int run = 1; run < RUNS; run++) {
                if (!first.equals(runOnce(dir))) {
                    throw SnippetRunException.unstable();
                }
            }
            return first;
        } finally {
            deleteRecursively(dir);
        }
    }

    private static boolean compile(Path dir, String code, int release) {
        try {
            Path source = Files.writeString(dir.resolve(MAIN_CLASS + ".java"), code);
            JavaCompiler compiler = Objects.requireNonNull(
                    ToolProvider.getSystemJavaCompiler(), "A JDK is required, no system Java compiler found");
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager files = compiler.getStandardFileManager(
                    diagnostics, null, StandardCharsets.UTF_8)) {
                List<String> options = List.of(
                        "--release", String.valueOf(release), "-proc:none", "-d", dir.toString());
                return compiler.getTask(null, files, diagnostics, options, null, files.getJavaFileObjects(source))
                        .call();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot compile in " + dir, e);
        }
    }

    private Outcome runOnce(Path dir) {
        try {
            Process process = new ProcessBuilder(
                            javaBinary(), "-Dstdout.encoding=UTF-8", "-cp", dir.toString(), MAIN_CLASS)
                    .start();
            process.getOutputStream().close();
            BoundedCapture stdout = capture(process.getInputStream(), process);
            BoundedCapture stderr = capture(process.getErrorStream(), process);
            Thread stdoutReader = Thread.ofVirtual().start(stdout);
            Thread stderrReader = Thread.ofVirtual().start(stderr);
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly().waitFor();
            }
            stdoutReader.join();
            stderrReader.join();
            return outcomeOf(process, finished, stdout, stderr);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot run snippet in " + dir, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running snippet in " + dir, e);
        }
    }

    private BoundedCapture capture(InputStream stream, Process process) {
        return new BoundedCapture(stream, maxOutputBytes, process::destroyForcibly);
    }

    private Outcome outcomeOf(Process process, boolean finished, BoundedCapture stdout, BoundedCapture stderr)
            throws IOException {
        if (stdout.exceeded() || stderr.exceeded()) {
            throw SnippetRunException.outputTooLarge(maxOutputBytes);
        }
        if (!finished) {
            throw SnippetRunException.timeout(timeout);
        }
        for (BoundedCapture capture : List.of(stdout, stderr)) {
            if (capture.failure() != null) {
                throw capture.failure();
            }
        }
        return outcomeOf(process.exitValue(), stdout.text(), stderr.text());
    }

    private static Outcome outcomeOf(int exitCode, String stdout, String stderr) {
        String output = withoutTrailingNewline(stdout.replace("\r\n", "\n"));
        if (exitCode == 0) {
            return new Output(output);
        }
        Matcher exception = UNCAUGHT_EXCEPTION.matcher(stderr);
        if (!exception.find()) {
            throw SnippetRunException.crashed(exitCode);
        }
        return new ThrownException(exception.group(1), output);
    }

    private static String withoutTrailingNewline(String text) {
        return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
    }

    private static String javaBinary() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory(TEMP_DIR_PREFIX);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create a temp directory", e);
        }
    }

    private static void deleteRecursively(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + dir, e);
        }
    }
}
