package io.github.mfedenko09.roastlands.core.session;

import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.mastery.MasteryLevel;
import io.github.mfedenko09.roastlands.core.profile.PlayerProfile;
import io.github.mfedenko09.roastlands.core.topic.Island;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProgressReporter {

    private final TopicGraph topics;
    private final ContentPack content;
    private final MasteryBoard masteryBoard;

    public ProgressReporter(TopicGraph topics, ContentPack content) {
        this.topics = topics;
        this.content = content;
        this.masteryBoard = new MasteryBoard(content.questions());
    }

    public ProgressReport report(PlayerProfile profile, LocalDate today, SessionLimits limits) {
        Map<String, Double> mastery = masteryBoard.masteryByTopic(profile.cards(), today);
        Set<String> reviewedTopics = content.questions().stream()
                .filter(question -> profile.cards().containsKey(question.id()))
                .map(Question::topic)
                .collect(Collectors.toSet());
        List<IslandProgress> islands = topics.islands().stream()
                .map(island -> islandProgress(island, mastery, reviewedTopics, profile))
                .toList();
        return new ProgressReport(islands, todayProgress(profile, today, limits));
    }

    private IslandProgress islandProgress(
            Island island, Map<String, Double> mastery, Set<String> reviewedTopics, PlayerProfile profile) {
        List<TopicProgress> progress = topics.topics().stream()
                .filter(topic -> topic.islandId().equals(island.id()))
                .map(topic -> topicProgress(topic, mastery, reviewedTopics.contains(topic.id()), profile))
                .toList();
        return new IslandProgress(island, progress);
    }

    private TopicProgress topicProgress(
            Topic topic, Map<String, Double> mastery, boolean reviewed, PlayerProfile profile) {
        double topicMastery = mastery.getOrDefault(topic.id(), 0.0);
        return TopicProgress.builder()
                .topic(topic)
                .level(MasteryLevel.of(topicMastery, reviewed))
                .mastery(topicMastery)
                .locked(!profile.unlockedTopics().contains(topic.id()))
                .weakPrerequisites(topic.prerequisites().stream()
                        .filter(id -> mastery.getOrDefault(id, 0.0) < TopicGraph.UNLOCK_THRESHOLD)
                        .toList())
                .build();
    }

    private SessionProgress todayProgress(PlayerProfile profile, LocalDate today, SessionLimits limits) {
        Set<String> knownIds = content.questions().stream().map(Question::id).collect(Collectors.toSet());
        int reviewedToday = (int) profile.cards().values().stream()
                .filter(card -> knownIds.contains(card.cardId()) && today.equals(card.lastReviewDate()))
                .count();
        int stillToDo = ReviewSession.start(profile, content, topics, today, limits).progress().total();
        return new SessionProgress(reviewedToday, reviewedToday + stillToDo);
    }
}
