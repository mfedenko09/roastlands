package io.github.mfedenko09.roastlands.core.topic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LoadedPacks {

    private final List<Island> islands = new ArrayList<>();
    private final List<Topic> topics = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();
    private final Map<TopicMapSubject, List<Path>> sources = new LinkedHashMap<>();
    private final Map<String, Path> packFilesById = new HashMap<>();

    void addProblem(Path file, String description) {
        problems.add(file + ": " + description);
    }

    void registerPack(String packId, Path packFile) {
        Path first = packFilesById.putIfAbsent(packId, packFile);
        if (first != null) {
            addProblem(packFile, "duplicate pack id \"" + packId + "\", already used in " + first);
        }
    }

    void addIsland(Island island, Path topicsFile) {
        islands.add(island);
        sources.computeIfAbsent(TopicMapSubject.island(island.id()), key -> new ArrayList<>()).add(topicsFile);
    }

    void addTopic(Topic topic, Path topicsFile) {
        topics.add(topic);
        sources.computeIfAbsent(TopicMapSubject.topic(topic.id()), key -> new ArrayList<>()).add(topicsFile);
    }

    List<Island> islands() {
        return islands;
    }

    List<Topic> topics() {
        return topics;
    }

    List<String> problems() {
        return problems;
    }

    String locate(TopicMapProblem problem) {
        List<String> files = sources.getOrDefault(problem.subject(), List.of()).stream().map(Path::toString).toList();
        return files.isEmpty() ? problem.text() : String.join(", ", files) + ": " + problem.text();
    }
}
