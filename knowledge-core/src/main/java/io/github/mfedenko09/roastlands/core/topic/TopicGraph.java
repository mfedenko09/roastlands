package io.github.mfedenko09.roastlands.core.topic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TopicGraph {

    public static final double UNLOCK_THRESHOLD = 0.6;

    private static final String CYCLE_SEPARATOR = " -> ";

    private final List<Island> islands;
    private final Map<String, Topic> topicsById;
    private final List<Topic> topologicalOrder;

    private TopicGraph(List<Island> islands, Map<String, Topic> topicsById, List<Topic> topologicalOrder) {
        this.islands = islands;
        this.topicsById = topicsById;
        this.topologicalOrder = topologicalOrder;
    }

    public static TopicGraph of(Collection<Island> islands, Collection<Topic> topics) {
        List<TopicMapProblem> problems = validate(islands, topics);
        if (!problems.isEmpty()) {
            throw new InvalidTopicMapException(problems);
        }
        Map<String, Topic> topicsById = new LinkedHashMap<>();
        topics.forEach(topic -> topicsById.put(topic.id(), topic));
        return new TopicGraph(List.copyOf(islands), topicsById, sortTopologically(topicsById));
    }

    private static List<TopicMapProblem> validate(Collection<Island> islands, Collection<Topic> topics) {
        List<TopicMapProblem> problems = new ArrayList<>();
        Set<String> islandIds = collectUniqueIslandIds(islands, problems);
        Map<String, Topic> topicsById = collectUniqueTopics(topics, problems);
        for (Topic topic : topicsById.values()) {
            if (!islandIds.contains(topic.islandId())) {
                problems.add(problem(topic, "unknown island \"" + topic.islandId() + "\""));
            }
            for (String prerequisite : topic.prerequisites()) {
                if (!topicsById.containsKey(prerequisite)) {
                    problems.add(problem(topic, "unknown prerequisite \"" + prerequisite + "\""));
                }
            }
        }
        problems.addAll(findCycles(topicsById));
        return problems;
    }

    public List<Island> islands() {
        return islands;
    }

    public List<Topic> topics() {
        return List.copyOf(topicsById.values());
    }

    public Set<String> rootTopicIds() {
        Set<String> roots = new LinkedHashSet<>();
        topicsById.values().stream()
                .filter(topic -> topic.prerequisites().isEmpty())
                .forEach(topic -> roots.add(topic.id()));
        return roots;
    }

    public List<Topic> topologicalOrder() {
        return topologicalOrder;
    }

    public Set<String> unlockable(Map<String, Double> mastery, Set<String> alreadyUnlocked) {
        Set<String> unlocked = new LinkedHashSet<>();
        for (Topic topic : topologicalOrder) {
            if (alreadyUnlocked.contains(topic.id()) || prerequisitesAreStrong(topic, mastery)) {
                unlocked.add(topic.id());
            }
        }
        return unlocked;
    }

    private static boolean prerequisitesAreStrong(Topic topic, Map<String, Double> mastery) {
        return topic.prerequisites().stream()
                .allMatch(prerequisite -> mastery.getOrDefault(prerequisite, 0.0) >= UNLOCK_THRESHOLD);
    }

    private static TopicMapProblem problem(Topic topic, String description) {
        return new TopicMapProblem(TopicMapSubject.topic(topic.id()), description);
    }

    private static Set<String> collectUniqueIslandIds(Collection<Island> islands, List<TopicMapProblem> problems) {
        Set<String> ids = new HashSet<>();
        for (Island island : islands) {
            if (!ids.add(island.id())) {
                problems.add(new TopicMapProblem(TopicMapSubject.island(island.id()), "duplicate island id"));
            }
        }
        return ids;
    }

    private static Map<String, Topic> collectUniqueTopics(Collection<Topic> topics, List<TopicMapProblem> problems) {
        Map<String, Topic> topicsById = new LinkedHashMap<>();
        for (Topic topic : topics) {
            if (topicsById.putIfAbsent(topic.id(), topic) != null) {
                problems.add(problem(topic, "duplicate topic id"));
            }
        }
        return topicsById;
    }

    private static List<TopicMapProblem> findCycles(Map<String, Topic> topicsById) {
        List<TopicMapProblem> problems = new ArrayList<>();
        Set<String> finished = new HashSet<>();
        for (String id : topicsById.keySet().stream().sorted().toList()) {
            visit(id, topicsById, new ArrayList<>(), finished, problems);
        }
        return problems;
    }

    private static void visit(
            String id,
            Map<String, Topic> topicsById,
            List<String> path,
            Set<String> finished,
            List<TopicMapProblem> problems) {
        int onPathAt = path.indexOf(id);
        if (onPathAt >= 0) {
            List<String> cycle = new ArrayList<>(path.subList(onPathAt, path.size()));
            cycle.add(id);
            problems.add(new TopicMapProblem(
                    TopicMapSubject.topic(id), "dependency cycle " + String.join(CYCLE_SEPARATOR, cycle)));
            return;
        }
        if (finished.contains(id)) {
            return;
        }
        path.add(id);
        topicsById.get(id).prerequisites().stream()
                .filter(topicsById::containsKey)
                .sorted()
                .forEach(prerequisite -> visit(prerequisite, topicsById, path, finished, problems));
        path.remove(path.size() - 1);
        finished.add(id);
    }

    private static List<Topic> sortTopologically(Map<String, Topic> topicsById) {
        List<Topic> ordered = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        List<Topic> remaining = new ArrayList<>(topicsById.values());
        remaining.sort(Comparator.comparing(Topic::id));
        while (!remaining.isEmpty()) {
            List<Topic> ready = remaining.stream()
                    .filter(topic -> placed.containsAll(topic.prerequisites()))
                    .toList();
            ready.forEach(topic -> placed.add(topic.id()));
            ordered.addAll(ready);
            remaining.removeAll(ready);
        }
        return List.copyOf(ordered);
    }
}
