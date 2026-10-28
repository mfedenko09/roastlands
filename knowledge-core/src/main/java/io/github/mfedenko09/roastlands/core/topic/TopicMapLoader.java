package io.github.mfedenko09.roastlands.core.topic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class TopicMapLoader {

    private static final String PACK_FILE = "pack.yaml";
    private static final String TOPICS_FILE = "topics.yaml";

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public TopicGraph load(Path packsRoot) {
        LoadedPacks loaded = new LoadedPacks();
        packDirectories(packsRoot).forEach(packDir -> readPack(packDir, loaded));
        if (!loaded.problems().isEmpty()) {
            throw new InvalidTopicMapException(InvalidTopicMapException.describe(loaded.problems()));
        }
        try {
            return TopicGraph.of(loaded.islands(), loaded.topics());
        } catch (InvalidTopicMapException e) {
            throw new InvalidTopicMapException(
                    InvalidTopicMapException.describe(e.problems().stream().map(loaded::locate).toList()));
        }
    }

    private static List<Path> packDirectories(Path packsRoot) {
        try (Stream<Path> children = Files.list(packsRoot)) {
            return children
                    .filter(child -> Files.isRegularFile(child.resolve(TOPICS_FILE)))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list packs in " + packsRoot, e);
        }
    }

    private void readPack(Path packDir, LoadedPacks loaded) {
        Path packPath = packDir.resolve(PACK_FILE);
        Path topicsPath = packDir.resolve(TOPICS_FILE);
        PackFile pack = parse(packPath, PackFile.class, loaded);
        TopicsFile topicsFile = parse(topicsPath, TopicsFile.class, loaded);
        if (pack == null || topicsFile == null) {
            return;
        }
        if (isBlank(pack.id()) || isBlank(pack.title()) || isBlank(pack.version()) || pack.minJavaVersion() == null) {
            loaded.addProblem(packPath, "pack needs id, title, version and minJavaVersion");
            return;
        }
        loaded.registerPack(pack.id(), packPath);
        orEmpty(topicsFile.islands()).forEach(island -> readIsland(island, topicsPath, loaded));
        orEmpty(topicsFile.topics()).forEach(entry -> readTopic(entry, pack, topicsPath, loaded));
    }

    private static void readIsland(Island island, Path topicsPath, LoadedPacks loaded) {
        if (isBlank(island.id()) || isBlank(island.title())) {
            loaded.addProblem(topicsPath, "island entry \"" + island.id() + "\" needs id and title");
            return;
        }
        loaded.addIsland(island, topicsPath);
    }

    private static void readTopic(TopicEntry entry, PackFile pack, Path topicsPath, LoadedPacks loaded) {
        if (isBlank(entry.id()) || isBlank(entry.title()) || isBlank(entry.island())) {
            loaded.addProblem(topicsPath, "topic entry \"" + entry.id() + "\" needs id, title and island");
            return;
        }
        loaded.addTopic(toTopic(entry, pack), topicsPath);
    }

    private <T> T parse(Path file, Class<T> type, LoadedPacks loaded) {
        try {
            T parsed = yaml.readValue(file.toFile(), type);
            if (parsed == null) {
                loaded.addProblem(file, "file is empty");
            }
            return parsed;
        } catch (JsonProcessingException e) {
            loaded.addProblem(file, e.getOriginalMessage() + " (line " + e.getLocation().getLineNr() + ")");
        } catch (IOException e) {
            loaded.addProblem(file, "cannot be read: " + e.getMessage());
        }
        return null;
    }

    private static Topic toTopic(TopicEntry entry, PackFile pack) {
        return Topic.builder()
                .id(entry.id())
                .title(entry.title())
                .islandId(entry.island())
                .prerequisites(orEmpty(entry.prerequisites()))
                .minJavaVersion(entry.minJavaVersion() != null ? entry.minJavaVersion() : pack.minJavaVersion())
                .keyPoints(orEmpty(entry.keyPoints()))
                .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> List<T> orEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
