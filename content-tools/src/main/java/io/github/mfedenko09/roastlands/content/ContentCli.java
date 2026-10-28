package io.github.mfedenko09.roastlands.content;

import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.ContentPackLoader;
import io.github.mfedenko09.roastlands.core.content.InvalidContentException;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import io.github.mfedenko09.roastlands.core.topic.TopicMapLoader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class ContentCli {

    private static final String VERIFY = "verify";
    private static final String USAGE = "usage: verify <packsRoot>";
    private static final String PACK_FILE = "pack.yaml";

    private ContentCli() {
    }

    public static void main(String[] args) {
        run(args);
    }

    static void run(String[] args) {
        if (args.length != 2 || !VERIFY.equals(args[0])) {
            throw new IllegalArgumentException(USAGE);
        }
        VerificationReport report = verify(Path.of(args[1]), new SnippetRunner());
        System.out.println(report.summary());
        if (!report.isSuccess()) {
            throw new InvalidContentException(report.failures());
        }
    }

    static VerificationReport verify(Path packsRoot, SnippetRunner runner) {
        TopicGraph topics = new TopicMapLoader().load(packsRoot);
        ContentPackLoader loader = new ContentPackLoader();
        List<ContentPack> packs = packDirectories(packsRoot).stream().map(dir -> loader.load(dir, topics)).toList();
        return new ContentVerifier(runner, Runtime.getRuntime().availableProcessors()).verify(packs);
    }

    private static List<Path> packDirectories(Path packsRoot) {
        try (Stream<Path> children = Files.list(packsRoot)) {
            return children.filter(child -> Files.isRegularFile(child.resolve(PACK_FILE))).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list packs in " + packsRoot, e);
        }
    }
}
