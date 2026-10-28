package io.github.mfedenko09.roastlands.core.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TopicGraphTests {

    private static final List<Island> ISLANDS = List.of(new Island("shore", "Shore"), new Island("grotto", "Grotto"));

    @Test
    void topologicalOrderPutsPrerequisitesFirstAndBreaksTiesById() {
        TopicGraph graph = TopicGraph.of(ISLANDS, List.of(
                topic("c", "b"),
                topic("b", "a"),
                topic("d"),
                topic("a")));

        assertThat(graph.topologicalOrder()).extracting(Topic::id).containsExactly("a", "d", "b", "c");
    }

    @Test
    void rootTopicsAreThoseWithoutPrerequisites() {
        TopicGraph graph = TopicGraph.of(ISLANDS, List.of(topic("c", "b"), topic("b", "a"), topic("d"), topic("a")));

        assertThat(graph.rootTopicIds()).containsExactlyInAnyOrder("a", "d");
    }

    @Test
    void cycleIsReportedWithItsPath() {
        assertThatThrownBy(() -> TopicGraph.of(ISLANDS, List.of(topic("A", "B"), topic("B", "A"))))
                .isInstanceOf(InvalidTopicMapException.class)
                .hasMessageContaining("topic A: dependency cycle A -> B -> A");
    }

    @Test
    void topicThatRequiresItselfIsACycle() {
        assertThatThrownBy(() -> TopicGraph.of(ISLANDS, List.of(topic("A", "A"))))
                .hasMessageContaining("topic A: dependency cycle A -> A");
    }

    @Test
    void longerCycleIsReportedOnceEvenWhenReachedFromTwoTopics() {
        List<TopicMapProblem> problems = problemsOf(ISLANDS, List.of(
                topic("A", "B"), topic("B", "C"), topic("C", "A"), topic("D", "A")));

        assertThat(problems).extracting(TopicMapProblem::text)
                .containsExactly("topic A: dependency cycle A -> B -> C -> A");
    }

    @Test
    void unknownPrerequisiteNamesTopicAndMissingId() {
        assertThatThrownBy(() -> TopicGraph.of(ISLANDS, List.of(topic("core.a", "core.typo"))))
                .hasMessageContaining("topic core.a: unknown prerequisite \"core.typo\"");
    }

    @Test
    void unknownIslandNamesTopicAndIsland() {
        Topic lost = Topic.builder().id("a").title("a").islandId("nowhere")
                .prerequisites(List.of()).keyPoints(List.of()).build();

        assertThatThrownBy(() -> TopicGraph.of(ISLANDS, List.of(lost)))
                .hasMessageContaining("topic a: unknown island \"nowhere\"");
    }

    @Test
    void duplicateTopicIdAndDuplicateIslandIdAreRejected() {
        List<Island> islands = List.of(new Island("shore", "One"), new Island("shore", "Two"));

        List<TopicMapProblem> problems = problemsOf(islands, List.of(topic("a"), topic("a")));

        assertThat(problems).extracting(TopicMapProblem::text)
                .containsExactlyInAnyOrder("island shore: duplicate island id", "topic a: duplicate topic id");
    }

    @Test
    void allProblemsAreThrownTogether() {
        assertThatThrownBy(() -> TopicGraph.of(ISLANDS, List.of(topic("a", "x"), topic("b", "y"), topic("c", "d"))))
                .isInstanceOf(InvalidTopicMapException.class)
                .hasMessageContaining("topic a: unknown prerequisite \"x\"")
                .hasMessageContaining("topic b: unknown prerequisite \"y\"")
                .hasMessageContaining("topic c: unknown prerequisite \"d\"");
    }

    @Test
    void onlyRootTopicsAreOpenWhenNothingIsLearned() {
        assertThat(objectContractGraph().unlockable(Map.of(), Set.of()))
                .containsExactlyInAnyOrder("core.types", "core.oop");
    }

    @Test
    void topicOpensWhenAllPrerequisitesAreStrongEnough() {
        Map<String, Double> mastery = Map.of("core.oop", 0.65, "core.types", 0.70);

        assertThat(objectContractGraph().unlockable(mastery, Set.of())).contains("core.object-contract");
    }

    @Test
    void oneWeakPrerequisiteKeepsTopicClosed() {
        Map<String, Double> mastery = Map.of("core.oop", 0.65, "core.types", 0.50);

        assertThat(objectContractGraph().unlockable(mastery, Set.of())).doesNotContain("core.object-contract");
    }

    @Test
    void thresholdItselfOpensTheTopicAndJustBelowDoesNot() {
        TopicGraph graph = objectContractGraph();

        assertThat(graph.unlockable(Map.of("core.oop", 0.6, "core.types", 0.6), Set.of()))
                .contains("core.object-contract");
        assertThat(graph.unlockable(Map.of("core.oop", 0.6, "core.types", 0.599), Set.of()))
                .doesNotContain("core.object-contract");
    }

    @Test
    void openedTopicStaysOpenWhenPrerequisiteMasteryDecays() {
        Map<String, Double> mastery = Map.of("core.oop", 0.65, "core.types", 0.10);

        assertThat(objectContractGraph().unlockable(mastery, Set.of("core.object-contract")))
                .contains("core.object-contract");
    }

    @Test
    void unknownIdsInAlreadyUnlockedAreIgnored() {
        assertThat(objectContractGraph().unlockable(Map.of(), Set.of("removed.topic")))
                .doesNotContain("removed.topic");
    }

    private static List<TopicMapProblem> problemsOf(List<Island> islands, List<Topic> topics) {
        return catchThrowableOfType(InvalidTopicMapException.class, () -> TopicGraph.of(islands, topics)).problems();
    }

    private static TopicGraph objectContractGraph() {
        return TopicGraph.of(ISLANDS, List.of(
                topic("core.types"),
                topic("core.oop"),
                topic("core.object-contract", "core.oop", "core.types")));
    }

    private static Topic topic(String id, String... prerequisites) {
        return Topic.builder()
                .id(id)
                .title(id)
                .islandId("shore")
                .prerequisites(List.of(prerequisites))
                .minJavaVersion(21)
                .keyPoints(List.of())
                .build();
    }
}
