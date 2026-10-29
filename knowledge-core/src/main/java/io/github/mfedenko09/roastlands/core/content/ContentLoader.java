package io.github.mfedenko09.roastlands.core.content;

import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import io.github.mfedenko09.roastlands.core.topic.TopicMapLoader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class ContentLoader {

    private static final String PACKS_DIR = "packs";
    private static final String PACK_FILE = "pack.yaml";
    private static final String PACK_ID_SEPARATOR = "+";

    public GameContent load(Path contentDir) {
        Path packsRoot = contentDir.resolve(PACKS_DIR);
        requirePacksFolder(packsRoot);
        TopicGraph topics = new TopicMapLoader().load(packsRoot);
        ContentPackLoader loader = new ContentPackLoader();
        List<ContentPack> packs = packDirectories(packsRoot).stream().map(dir -> loader.load(dir, topics)).toList();
        ContentPack merged = new ContentPack(
                String.join(PACK_ID_SEPARATOR, packs.stream().map(ContentPack::id).toList()),
                packs.stream().flatMap(pack -> pack.questions().stream()).toList());
        return new GameContent(topics, merged);
    }

    private static void requirePacksFolder(Path packsRoot) {
        if (!Files.isDirectory(packsRoot)) {
            throw new InvalidContentException(List.of(
                    "Cannot find " + packsRoot.toAbsolutePath() + ", pass the content folder with --content <dir>"));
        }
    }

    private static List<Path> packDirectories(Path packsRoot) {
        try (Stream<Path> children = Files.list(packsRoot)) {
            return children.filter(child -> Files.isRegularFile(child.resolve(PACK_FILE))).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list packs in " + packsRoot, e);
        }
    }
}
