package io.github.mfedenko09.roastlands.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class BoundedCapture implements Runnable {

    private static final int CHUNK_SIZE = 8192;

    private final InputStream stream;
    private final int limit;
    private final Runnable onExceeded;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private volatile boolean exceeded;
    private volatile IOException failure;

    BoundedCapture(InputStream stream, int limit, Runnable onExceeded) {
        this.stream = stream;
        this.limit = limit;
        this.onExceeded = onExceeded;
    }

    @Override
    public void run() {
        byte[] chunk = new byte[CHUNK_SIZE];
        try (stream) {
            int read = stream.read(chunk);
            while (read != -1) {
                if (buffer.size() + read > limit) {
                    exceeded = true;
                    onExceeded.run();
                    return;
                }
                buffer.write(chunk, 0, read);
                read = stream.read(chunk);
            }
        } catch (IOException e) {
            failure = e;
        }
    }

    boolean exceeded() {
        return exceeded;
    }

    IOException failure() {
        return failure;
    }

    String text() {
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
