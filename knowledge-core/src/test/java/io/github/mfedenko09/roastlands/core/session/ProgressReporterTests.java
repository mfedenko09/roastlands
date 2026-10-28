package io.github.mfedenko09.roastlands.core.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import io.github.mfedenko09.roastlands.core.content.TextOption;
import io.github.mfedenko09.roastlands.core.mastery.MasteryLevel;
import io.github.mfedenko09.roastlands.core.profile.PlayerProfile;
import io.github.mfedenko09.roastlands.core.srs.CardState;
import io.github.mfedenko09.roastlands.core.topic.Island;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProgressReporterTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    private final TopicGraph topics = TopicGraph.of(
            List.of(new Island("shore", "Shore"), new Island("cave", "Cave")),
            List.of(topic("a", "shore"), topic("b", "cave", "a")));
    private final ContentPack content = new ContentPack("test", List.of(
            question("a-1", "a"), question("a-2", "a"), question("b-1", "b")));
    private final ProgressReporter reporter = new ProgressReporter(topics, content);

    @Test
    void freshProfileHasNewTopicsLockedBehindWeakPrerequisites() {
        ProgressReport report = reporter.report(PlayerProfile.empty(topics.rootTopicIds()), TODAY,
                SessionLimits.DEFAULT);

        assertThat(report.islands()).extracting(island -> island.island().id()).containsExactly("shore", "cave");
        TopicProgress a = report.islands().get(0).topics().get(0);
        TopicProgress b = report.islands().get(1).topics().get(0);
        assertThat(a.level()).isEqualTo(MasteryLevel.NEW);
        assertThat(a.locked()).isFalse();
        assertThat(b.locked()).isTrue();
        assertThat(b.weakPrerequisites()).containsExactly("a");
        assertThat(report.today()).isEqualTo(new SessionProgress(0, 2));
    }

    @Test
    void todayCountsCardsReviewedTodayPlusWhatIsLeftInTheQueue() {
        CardState reviewed = CardState.builder().cardId("a-1").repetitions(1).easeFactor(2.5).intervalDays(1)
                .dueDate(TODAY.plusDays(1)).firstSeenDate(TODAY).lastReviewDate(TODAY).build();
        PlayerProfile profile = PlayerProfile.empty(topics.rootTopicIds()).toBuilder()
                .cards(Map.of("a-1", reviewed)).build();

        ProgressReport report = reporter.report(profile, TODAY, SessionLimits.DEFAULT);

        assertThat(report.today()).isEqualTo(new SessionProgress(1, 2));
        TopicProgress a = report.islands().get(0).topics().get(0);
        assertThat(a.level()).isEqualTo(MasteryLevel.WARM);
        assertThat(a.mastery()).isEqualTo(0.5);
    }

    @Test
    void unlockedTopicHasNoLockAndCardsOfRemovedQuestionsAreNotCounted() {
        CardState removed = CardState.builder().cardId("gone").repetitions(1).easeFactor(2.5).intervalDays(1)
                .dueDate(TODAY.plusDays(1)).firstSeenDate(TODAY).lastReviewDate(TODAY).build();
        PlayerProfile profile = PlayerProfile.empty(Set.of("a", "b")).toBuilder()
                .cards(Map.of("gone", removed)).build();

        ProgressReport report = reporter.report(profile, TODAY, SessionLimits.DEFAULT);

        assertThat(report.islands().get(1).topics().get(0).locked()).isFalse();
        assertThat(report.today().done()).isZero();
    }

    private static Topic topic(String id, String island, String... prerequisites) {
        return Topic.builder().id(id).title(id).islandId(island).prerequisites(List.of(prerequisites))
                .minJavaVersion(8).keyPoints(List.of()).build();
    }

    private static Question question(String id, String topic) {
        return Question.builder().id(id).topic(topic).type(QuestionType.SINGLE_CHOICE).difficulty(1)
                .prompt("p").options(List.of(new TextOption("a"), new TextOption("b")))
                .correct(new SingleIndex(1)).explanation("e").reference("JLS 1").minJavaVersion(8).source("t")
                .build();
    }
}
