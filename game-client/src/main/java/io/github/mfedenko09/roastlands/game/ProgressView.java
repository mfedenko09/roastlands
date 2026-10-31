package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.content.GameContent;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.profile.PlayerProfile;
import io.github.mfedenko09.roastlands.core.progress.Levels;
import io.github.mfedenko09.roastlands.core.progress.StreakRules;
import io.github.mfedenko09.roastlands.core.session.IslandProgress;
import io.github.mfedenko09.roastlands.core.session.ProgressReport;
import io.github.mfedenko09.roastlands.core.session.ProgressReporter;
import io.github.mfedenko09.roastlands.core.session.SessionLimits;
import io.github.mfedenko09.roastlands.core.session.SessionProgress;
import io.github.mfedenko09.roastlands.core.session.TopicProgress;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

final class ProgressView {

    private static final int PERCENT = 100;
    private static final String TODAY = "Today";
    private static final String NO_QUESTIONS = "No questions yet";
    private static final String NOTHING_NEEDED = "";
    private static final double ROUNDING_TOLERANCE = 1e-9;

    private final Levels levels = new Levels();
    private final StreakRules streakRules = new StreakRules();
    private final ProgressReporter reporter;
    private final LocalDate today;
    private final Set<String> topicsWithQuestions;
    private final int doneBeforeSession;

    ProgressView(GameContent content, LocalDate today, PlayerProfile startProfile) {
        this.reporter = new ProgressReporter(content.topics(), content.pack());
        this.today = today;
        this.topicsWithQuestions = content.pack().questions().stream()
                .map(Question::topic)
                .collect(Collectors.toSet());
        this.doneBeforeSession = report(startProfile).today().done();
    }

    List<IslandView> islands(PlayerProfile profile) {
        ProgressReport report = report(profile);
        Map<String, Double> masteryByTopic = report.islands().stream()
                .flatMap(island -> island.topics().stream())
                .collect(Collectors.toMap(topic -> topic.topic().id(), TopicProgress::mastery));
        return report.islands().stream().map(island -> islandView(island, masteryByTopic)).toList();
    }

    TodayView today(SessionProgress session) {
        int done = doneBeforeSession + session.done();
        int total = doneBeforeSession + session.total();
        return done >= total ? new TodayView(TODAY, true) : new TodayView(TODAY + " " + done + "/" + total, false);
    }

    PlayerStatsView stats(PlayerProfile profile) {
        String level = "Lv " + levels.levelOf(profile.xp()) + PanelHeader.SEPARATOR;
        return new PlayerStatsView(level, String.valueOf(streakRules.currentOn(profile.streak(), today)));
    }

    private ProgressReport report(PlayerProfile profile) {
        return reporter.report(profile, today, SessionLimits.DEFAULT);
    }

    private IslandView islandView(IslandProgress island, Map<String, Double> masteryByTopic) {
        List<TopicView> topics = island.topics().stream().map(topic -> topicView(topic, masteryByTopic)).toList();
        return new IslandView(island.island().title(), summary(island), topics);
    }

    private TopicView topicView(TopicProgress topic, Map<String, Double> masteryByTopic) {
        return TopicView.builder()
                .title(topic.topic().title())
                .level(topic.level())
                .progress(hasQuestions(topic) ? percentText(topic.mastery()) : NO_QUESTIONS)
                .locked(topic.locked())
                .needs(topic.locked() ? needs(topic, masteryByTopic) : NOTHING_NEEDED)
                .build();
    }

    private boolean hasQuestions(TopicProgress topic) {
        return topicsWithQuestions.contains(topic.topic().id());
    }

    private String summary(IslandProgress island) {
        List<TopicProgress> open = island.topics().stream().filter(topic -> !topic.locked()).toList();
        String openCount = open.size() + "/" + island.topics().size() + " topics open";
        OptionalDouble average = open.stream()
                .filter(this::hasQuestions)
                .mapToDouble(TopicProgress::mastery)
                .average();
        return average.isPresent() ? percentText(average.getAsDouble()) + PanelHeader.SEPARATOR + openCount : openCount;
    }

    private String needs(TopicProgress topic, Map<String, Double> masteryByTopic) {
        List<String> prerequisites = topic.topic().prerequisites();
        if (prerequisites.isEmpty()) {
            return NOTHING_NEEDED;
        }
        String target = percentText(TopicGraph.UNLOCK_THRESHOLD);
        return "Needs: " + prerequisites.stream()
                .map(id -> id + " " + requirement(id, masteryByTopic.get(id), target))
                .collect(Collectors.joining(", "));
    }

    private String requirement(String topicId, double mastery, String target) {
        if (!topicsWithQuestions.contains(topicId)) {
            return NO_QUESTIONS;
        }
        return (int) Math.floor(mastery * PERCENT + ROUNDING_TOLERANCE) + "% / " + target;
    }

    private static String percentText(double mastery) {
        return Math.round(mastery * PERCENT) + "%";
    }
}
