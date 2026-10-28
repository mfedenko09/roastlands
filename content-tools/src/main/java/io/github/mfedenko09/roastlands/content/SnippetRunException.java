package io.github.mfedenko09.roastlands.content;

import java.time.Duration;

public class SnippetRunException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final long MILLIS_PER_SECOND = 1000;
    private static final int BYTES_PER_MEGABYTE = 1024 * 1024;

    private SnippetRunException(String message) {
        super(message);
    }

    static SnippetRunException timeout(Duration timeout) {
        boolean wholeSeconds = timeout.toMillis() % MILLIS_PER_SECOND == 0;
        return new SnippetRunException(
                "timeout after " + (wholeSeconds ? timeout.toSeconds() + " s" : timeout.toMillis() + " ms"));
    }

    static SnippetRunException outputTooLarge(int limitBytes) {
        boolean wholeMegabytes = limitBytes % BYTES_PER_MEGABYTE == 0;
        return new SnippetRunException("output exceeds "
                + (wholeMegabytes ? limitBytes / BYTES_PER_MEGABYTE + " MB" : limitBytes + " bytes"));
    }

    static SnippetRunException unstable() {
        return new SnippetRunException("outcome is not stable between runs");
    }

    static SnippetRunException crashed(int exitCode) {
        return new SnippetRunException("exit code " + exitCode + " without an uncaught exception in main");
    }
}
