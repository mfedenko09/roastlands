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
    private static final String COVERAGE = "coverage";
    private static final String USAGE = "usage: verify|coverage <packsRoot>";
    private static final String PACK_FILE = "pack.yaml";

    private ContentCli() {
    }

    public static void main(String[] args) {
        run(args);
    }

    static void run(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(USAGE);
        }
        Path packsRoot = Path.of(args[1]);
        switch (args[0]) {
            case VERIFY -> verifyAndReport(packsRoot);
            case COVERAGE -> System.out.print(coverage(packsRoot));
            default -> throw new IllegalArgumentException(USAGE);
        }
    }

    private static void verifyAndReport(Path packsRoot) {
        VerificationReport report = verify(packsRoot, new SnippetRunner());
        System.out.println(report.summary());
        if (!report.isSuccess()) {
            throw new InvalidContentException(report.failures());
        }
    }

    static VerificationReport verify(Path packsRoot, SnippetRunner runner) {
        List<ContentPack> packs = loadPacks(packsRoot, new TopicMapLoader().load(packsRoot));
        return new ContentVerifier(runner, Runtime.getRuntime().availableProcessors()).verify(packs);
    }

    static String coverage(Path packsRoot) {
        TopicGraph topics = new TopicMapLoader().load(packsRoot);
        return CoverageReport.render(topics, loadPacks(packsRoot, topics));
    }

    private static List<ContentPack> loadPacks(Path packsRoot, TopicGraph topics) {
        ContentPackLoader loader = new ContentPackLoader();
        return packDirectories(packsRoot).stream().map(dir -> loader.load(dir, topics)).toList();
    }

    private static List<Path> packDirectories(Path packsRoot) {
        try (Stream<Path> children = Files.list(packsRoot)) {
            return children.filter(child -> Files.isRegularFile(child.resolve(PACK_FILE))).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list packs in " + packsRoot, e);
        }
    }
}
