package io.github.mfedenko09.roastlands.content;

import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import io.github.mfedenko09.roastlands.core.topic.TopicMapLoader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TopicMapCli {

    private static final String GENERATE = "generate";
    private static final String VERIFY = "verify";
    private static final String USAGE = "usage: generate <packsRoot> <outputFile> | verify <packsRoot>";

    private TopicMapCli() {
    }

    public static void main(String[] args) {
        run(args);
    }

    static void run(String[] args) {
        if (args.length == 2 && VERIFY.equals(args[0])) {
            new TopicMapLoader().load(Path.of(args[1]));
        } else if (args.length == 3 && GENERATE.equals(args[0])) {
            generate(Path.of(args[1]), Path.of(args[2]));
        } else {
            throw new IllegalArgumentException(USAGE);
        }
    }

    private static void generate(Path packsRoot, Path outputFile) {
        TopicGraph graph = new TopicMapLoader().load(packsRoot);
        String markdown = TopicMapDocument.render(graph);
        try {
            Files.createDirectories(outputFile.toAbsolutePath().getParent());
            Files.writeString(outputFile, markdown);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + outputFile, e);
        }
    }
}
