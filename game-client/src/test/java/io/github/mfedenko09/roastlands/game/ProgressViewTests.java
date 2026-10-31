package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.answer.Confidence;
import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.GameContent;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import io.github.mfedenko09.roastlands.core.content.TextOption;
import io.github.mfedenko09.roastlands.core.mastery.MasteryLevel;
import io.github.mfedenko09.roastlands.core.profile.PlayerProfile;
import io.github.mfedenko09.roastlands.core.profile.Streak;
import io.github.mfedenko09.roastlands.core.session.ReviewSession;
import io.github.mfedenko09.roastlands.core.session.SessionLimits;
import io.github.mfedenko09.roastlands.core.session.SessionProgress;
import io.github.mfedenko09.roastlands.core.srs.CardState;
import io.github.mfedenko09.roastlands.core.topic.Island;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProgressViewTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    private final TopicGraph topics = TopicGraph.of(
            List.of(new Island("shore", "Shore"), new Island("cave", "Cave")),
            List.of(topic("a", "shore"), topic("b", "shore"), topic("empty", "shore"),
                    topic("c", "cave", "a", "b"), topic("d", "cave", "empty")));
    private final ContentPack content = new ContentPack("test", Stream.of(
                    questions("a", 20), questions("b", 10), questions("c", 2), questions("d", 1))
            .flatMap(List::stream)
            .toList());
    private final ProgressView view = new ProgressView(new GameContent(topics, content), TODAY, fresh());

    @Test
    void statsShowTheLevelFromXpAndTheCurrentStreak() {
        PlayerProfile profile = fresh().toBuilder().xp(400).streak(new Streak(5, 7, TODAY)).build();

        assertThat(view.stats(profile)).isEqualTo(new PlayerStatsView("Lv 3  |  ", "5"));
    }

    @Test
    void statsShowZeroAfterASkippedDayAndKeepTheProfileAsItIs() {
        Streak broken = new Streak(5, 7, TODAY.minusDays(2));
        PlayerProfile profile = fresh().toBuilder().streak(broken).build();

        assertThat(view.stats(profile).streak()).isEqualTo("0");
        assertThat(profile.streak()).isEqualTo(broken);
    }

    @Test
    void freshPlayerIsLevelOneWithNoStreak() {
        assertThat(view.stats(fresh())).isEqualTo(new PlayerStatsView("Lv 1  |  ", "0"));
    }

    @Test
    void islandsKeepTheOrderOfTheTopicGraph() {
        assertThat(view.islands(fresh())).extracting(IslandView::title).containsExactly("Shore", "Cave");
    }

    @Test
    void reviewedTopicShowsLevelAndRoundedMastery() {
        TopicView a = topicOf(view.islands(profileWith(9, 7)), "a");

        assertThat(a.level()).isEqualTo(MasteryLevel.WARM);
        assertThat(a.progress()).isEqualTo("45%");
        assertThat(a.locked()).isFalse();
        assertThat(a.needs()).isEmpty();
    }

    @Test
    void lockedTopicListsEveryPrerequisiteWithCurrentAndTargetPercent() {
        TopicView c = topicOf(view.islands(profileWith(9, 7)), "c");

        assertThat(c.locked()).isTrue();
        assertThat(c.needs()).isEqualTo("Needs: a 45% / 60%, b 70% / 60%");
    }

    @Test
    void lockedTopicWithoutPrerequisitesNeedsNothing() {
        PlayerProfile profile = PlayerProfile.empty(Set.of("b", "empty"));

        TopicView a = topicOf(view.islands(profile), "a");

        assertThat(a.locked()).isTrue();
        assertThat(a.needs()).isEmpty();
    }

    @Test
    void topicWithoutQuestionsSaysSoInsteadOfZeroPercent() {
        TopicView empty = topicOf(view.islands(fresh()), "empty");

        assertThat(empty.level()).isEqualTo(MasteryLevel.NEW);
        assertThat(empty.progress()).isEqualTo("No questions yet");
    }

    @Test
    void unreviewedTopicWithQuestionsShowsZeroPercent() {
        assertThat(topicOf(view.islands(fresh()), "a").progress()).isEqualTo("0%");
    }

    @Test
    void islandSummaryAveragesOpenTopicsWithQuestionsAndCountsOpenTopics() {
        IslandView shore = view.islands(profileWith(10, 7)).get(0);

        assertThat(shore.summary()).isEqualTo("60%  |  3/3 topics open");
    }

    @Test
    void lockedTopicsDoNotChangeTheIslandAverage() {
        PlayerProfile profile = profileWith(9, 7).toBuilder().unlockedTopics(Set.of("a", "empty")).build();

        IslandView shore = view.islands(profile).get(0);

        assertThat(shore.summary()).isEqualTo("45%  |  2/3 topics open");
    }

    @Test
    void islandWithoutOpenTopicsHasNoAverage() {
        assertThat(view.islands(fresh()).get(1).summary()).isEqualTo("0/2 topics open");
    }

    @Test
    void islandWithOnlyEmptyOpenTopicsHasNoAverage() {
        PlayerProfile profile = PlayerProfile.empty(Set.of("empty"));

        assertThat(view.islands(profile).get(0).summary()).isEqualTo("1/3 topics open");
    }

    @Test
    void lockedTopicNamesAPrerequisiteWithoutQuestions() {
        assertThat(topicOf(view.islands(fresh()), "d").needs()).isEqualTo("Needs: empty No questions yet");
    }

    @Test
    void needsRoundsDownSoItNeverPromisesAMetRequirement() {
        TopicGraph graph = TopicGraph.of(List.of(new Island("shore", "Shore")),
                List.of(topic("a", "shore"), topic("b", "shore", "a")));
        GameContent pack = new GameContent(graph, new ContentPack("test", questions("a", 200)));
        PlayerProfile profile = PlayerProfile.empty(Set.of("a")).toBuilder().cards(cards("a", 119)).build();

        TopicView locked = topicOf(new ProgressView(pack, TODAY, profile).islands(profile), "b");

        assertThat(locked.needs()).isEqualTo("Needs: a 59% / 60%");
    }

    @Test
    void needsKeepsAWholePercentThatDoubleArithmeticWouldDropBelowIt() {
        TopicGraph graph = TopicGraph.of(List.of(new Island("shore", "Shore")),
                List.of(topic("a", "shore"), topic("b", "shore", "a")));
        GameContent pack = new GameContent(graph, new ContentPack("test", questions("a", 50)));
        PlayerProfile profile = PlayerProfile.empty(Set.of("a")).toBuilder().cards(cards("a", 29)).build();

        TopicView locked = topicOf(new ProgressView(pack, TODAY, profile).islands(profile), "b");

        assertThat(locked.needs()).isEqualTo("Needs: a 58% / 60%");
    }

    @Test
    void todayAddsAnswersFromEarlierSessionsToThisSession() {
        PlayerProfile profile = PlayerProfile.empty(Set.of("a")).toBuilder().cards(cards("a", 2)).build();
        ProgressView restarted = new ProgressView(new GameContent(topics, content), TODAY, profile);

        assertThat(restarted.today(new SessionProgress(1, 3))).isEqualTo(new TodayView("Today 3/5", false));
    }

    @Test
    void todayIsFinishedAfterTheLastQuestion() {
        assertThat(view.today(new SessionProgress(3, 3))).isEqualTo(new TodayView("Today", true));
    }

    @Test
    void todayIsFinishedWhenThereIsNothingToDo() {
        assertThat(view.today(new SessionProgress(0, 0))).isEqualTo(new TodayView("Today", true));
    }

    @Test
    void backlogAboveTheSessionLimitKeepsTheTotalAndEndsFinished() {
        ContentPack backlog = new ContentPack("test", Stream.of(questions("a", 40), questions("b", 10))
                .flatMap(List::stream)
                .toList());
        Map<String, CardState> overdue = questions("a", 40).stream()
                .map(question -> CardState.builder().cardId(question.id()).repetitions(1).easeFactor(2.5)
                        .intervalDays(1).dueDate(TODAY.minusDays(1)).firstSeenDate(TODAY.minusDays(9))
                        .lastReviewDate(TODAY.minusDays(9)).build())
                .collect(Collectors.toMap(CardState::cardId, Function.identity()));
        PlayerProfile profile = fresh().toBuilder().cards(overdue).build();
        ProgressView backlogView = new ProgressView(new GameContent(topics, backlog), TODAY, profile);
        ReviewSession session = ReviewSession.start(profile, backlog, topics, TODAY, SessionLimits.DEFAULT);
        assertThat(backlogView.today(session.progress())).isEqualTo(new TodayView("Today 0/20", false));

        IntStream.range(0, 5).forEach(index -> answerCorrectly(session));
        assertThat(backlogView.today(session.progress())).isEqualTo(new TodayView("Today 5/20", false));

        IntStream.range(0, 15).forEach(index -> answerCorrectly(session));
        assertThat(session.isFinished()).isTrue();
        assertThat(backlogView.today(session.progress())).isEqualTo(new TodayView("Today", true));
    }

    @Test
    void topicOpenedByAnAnswerDoesNotGrowTodaysTotal() {
        TopicGraph graph = TopicGraph.of(List.of(new Island("shore", "Shore")),
                List.of(topic("a", "shore"), topic("b", "shore", "a")));
        ContentPack pack = new ContentPack("test", Stream.of(questions("a", 1), questions("b", 1))
                .flatMap(List::stream)
                .toList());
        PlayerProfile profile = PlayerProfile.empty(graph.rootTopicIds());
        ProgressView unlocking = new ProgressView(new GameContent(graph, pack), TODAY, profile);
        ReviewSession session = ReviewSession.start(profile, pack, graph, TODAY, SessionLimits.DEFAULT);

        answerCorrectly(session);

        assertThat(session.profile().unlockedTopics()).contains("b");
        assertThat(unlocking.today(session.progress())).isEqualTo(new TodayView("Today", true));
    }

    private PlayerProfile fresh() {
        return PlayerProfile.empty(topics.rootTopicIds());
    }

    private PlayerProfile profileWith(int reviewedOfA, int reviewedOfB) {
        Map<String, CardState> cards = Stream.concat(
                        cards("a", reviewedOfA).values().stream(), cards("b", reviewedOfB).values().stream())
                .collect(Collectors.toMap(CardState::cardId, Function.identity()));
        return fresh().toBuilder().cards(cards).build();
    }

    private static void answerCorrectly(ReviewSession session) {
        session.answer(Set.of(0), Confidence.HIGH);
        session.next();
    }

    private static TopicView topicOf(List<IslandView> islands, String title) {
        return islands.stream()
                .flatMap(island -> island.topics().stream())
                .filter(topic -> topic.title().equals(title))
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, CardState> cards(String topic, int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> CardState.builder().cardId(topic + "-" + index).repetitions(1).easeFactor(2.5)
                        .intervalDays(1).dueDate(TODAY.plusDays(1)).firstSeenDate(TODAY).lastReviewDate(TODAY).build())
                .collect(Collectors.toMap(CardState::cardId, Function.identity()));
    }

    private static Topic topic(String id, String islandId, String... prerequisites) {
        return Topic.builder().id(id).title(id).islandId(islandId).prerequisites(Arrays.asList(prerequisites))
                .minJavaVersion(21).keyPoints(List.of()).build();
    }

    private static List<Question> questions(String topic, int count) {
        return IntStream.range(0, count).mapToObj(index -> question(topic + "-" + index, topic)).toList();
    }

    private static Question question(String id, String topic) {
        return Question.builder().id(id).topic(topic).type(QuestionType.SINGLE_CHOICE).difficulty(1).prompt("?")
                .options(List.of(new TextOption("x"), new TextOption("y"))).correct(new SingleIndex(0))
                .explanation("e").reference("r").build();
    }
}
