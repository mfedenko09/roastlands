package io.github.mfedenko09.roastlands.core.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mfedenko09.roastlands.core.answer.Confidence;
import io.github.mfedenko09.roastlands.core.content.Bool;
import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.IndexSet;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import io.github.mfedenko09.roastlands.core.content.TextOption;
import io.github.mfedenko09.roastlands.core.profile.PlayerProfile;
import io.github.mfedenko09.roastlands.core.profile.Streak;
import io.github.mfedenko09.roastlands.core.srs.CardState;
import io.github.mfedenko09.roastlands.core.topic.Island;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ReviewSessionTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_2 = LocalDate.of(2026, 11, 2);
    private static final long SLOW_CI_LIMIT_MILLIS = 500;
    private static final String TYPES = "core.types";
    private static final String OOP = "core.oop";
    private static final String CONTRACT = "core.object-contract";

    private final TopicGraph topics = TopicGraph.of(
            List.of(new Island("core", "Core")),
            List.of(topic(TYPES), topic(OOP), topic(CONTRACT, OOP, TYPES)));

    @Test
    void dueCardsComeFirstThenNewOnesCappedByLimits() {
        List<Question> questions = new ArrayList<>();
        IntStream.rangeClosed(1, 3).forEach(n -> questions.add(question("due-" + n, TYPES, 1)));
        IntStream.rangeClosed(1, 10).forEach(n -> questions.add(question(String.format("new-%02d", n), TYPES, 1)));
        PlayerProfile profile = profileWith(
                Set.of(TYPES, OOP), dueCard("due-1", TODAY.minusDays(1)), dueCard("due-2", TODAY),
                dueCard("due-3", TODAY.minusDays(3)));

        ReviewSession session = start(profile, questions, new SessionLimits(15, 5));

        assertThat(queueIds(profile, questions, new SessionLimits(15, 5))).containsExactly(
                "due-3", "due-1", "due-2", "new-01", "new-02", "new-03", "new-04", "new-05");
        assertThat(session.progress()).isEqualTo(new SessionProgress(0, 8));
    }

    @Test
    void newQuestionsFollowMapOrderThenDifficultyThenId() {
        List<Question> questions = List.of(
                question("o-1", OOP, 1), question("t-hard", TYPES, 3), question("t-b", TYPES, 1),
                question("t-a", TYPES, 1), question("c-1", CONTRACT, 1));
        PlayerProfile profile = PlayerProfile.empty(Set.of(TYPES, OOP));

        assertThat(queueIds(profile, questions, SessionLimits.DEFAULT)).containsExactly("o-1", "t-a", "t-b", "t-hard");
    }

    @Test
    void lockedTopicQuestionsAndAlreadyKnownCardsAreNotOffered() {
        List<Question> questions = List.of(question("t-1", TYPES, 1), question("t-2", TYPES, 1),
                question("c-1", CONTRACT, 1));
        PlayerProfile profile = profileWith(Set.of(TYPES), dueCard("t-1", TODAY.plusDays(4)));

        assertThat(queueIds(profile, questions, SessionLimits.DEFAULT)).containsExactly("t-2");
    }

    @Test
    void cardWithoutQuestionInContentIsSkipped() {
        PlayerProfile profile = profileWith(Set.of(TYPES), dueCard("deleted", TODAY.minusDays(1)));

        List<Question> questions = List.of(question("t-1", TYPES, 1));
        ReviewSession session = start(profile, questions, SessionLimits.DEFAULT);

        assertThat(queueIds(profile, questions, SessionLimits.DEFAULT)).containsExactly("t-1");
        assertThat(session.profile().cards()).containsKey("deleted");
    }

    @Test
    void rightAnswerWithHighConfidenceSchedulesNewCardForTomorrowAndRaisesMastery() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES, OOP)),
                List.of(question("core.types.integer-cache", TYPES, 1), question("t-2", TYPES, 1)),
                SessionLimits.DEFAULT);

        AnswerResult result = session.answer(Set.of(1), Confidence.HIGH);

        assertThat(result.correct()).isTrue();
        assertThat(result.correctAnswer()).isEqualTo(new SingleIndex(1));
        assertThat(result.explanation()).isEqualTo("because core.types.integer-cache");
        assertThat(result.reference()).isEqualTo("JLS 1");
        assertThat(result.topicId()).isEqualTo(TYPES);
        assertThat(result.masteryBefore()).isZero();
        assertThat(result.masteryAfter()).isEqualTo(0.5);
        assertThat(result.topicsUnlocked()).isEmpty();
        CardState card = session.profile().cards().get("core.types.integer-cache");
        assertThat(card.dueDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(card.lastReviewDate()).isEqualTo(TODAY);
    }

    @Test
    void wrongAnswerResetsTheCardAndIsFlaggedIncorrect() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)), List.of(question("t-1", TYPES, 1)), SessionLimits.DEFAULT);

        AnswerResult result = session.answer(Set.of(0), Confidence.LOW);

        assertThat(result.correct()).isFalse();
        assertThat(session.profile().cards().get("t-1").lapses()).isEqualTo(1);
        assertThat(session.profile().cards().get("t-1").dueDate()).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void wrongAnswerDoesNotNeedConfidence() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)), List.of(question("t-1", TYPES, 1)), SessionLimits.DEFAULT);

        assertThat(session.answer(Set.of(0), null).correct()).isFalse();
    }

    @Test
    void multiChoiceNeedsTheWholeSet() {
        Question multi = question("m-1", TYPES, 1).toBuilder()
                .type(QuestionType.MULTI_CHOICE).correct(new IndexSet(Set.of(0, 2))).build();
        ReviewSession session = start(PlayerProfile.empty(Set.of(TYPES)), List.of(multi), SessionLimits.DEFAULT);

        assertThat(session.answer(Set.of(0, 2), Confidence.HIGH).correct()).isTrue();
    }

    @Test
    void trueFalseQuestionIsAnsweredWithABoolean() {
        Question tf = question("tf-1", TYPES, 1).toBuilder()
                .type(QuestionType.TRUE_FALSE).correct(new Bool(true)).build();
        ReviewSession session = start(PlayerProfile.empty(Set.of(TYPES)), List.of(tf), SessionLimits.DEFAULT);

        AnswerResult result = session.answerTrueFalse(true, Confidence.HIGH);

        assertThat(result.correct()).isTrue();
        assertThat(result.correctAnswer()).isEqualTo(new Bool(true));
    }

    @Test
    void wrongAnswerKindLeavesTheQuestionUnanswered() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)), List.of(question("t-1", TYPES, 1)), SessionLimits.DEFAULT);

        assertThatThrownBy(() -> session.answerTrueFalse(true, Confidence.HIGH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(session.answer(Set.of(1), Confidence.HIGH).correct()).isTrue();
    }

    @Test
    void answerThatRaisesPrerequisiteMasteryReportsAndStoresUnlockedTopic() {
        List<Question> questions = new ArrayList<>();
        IntStream.rangeClosed(1, 13).forEach(n -> questions.add(question(String.format("oop-%02d", n), OOP, 1)));
        IntStream.rangeClosed(1, 10).forEach(n -> questions.add(question("types-" + n, TYPES, 1)));
        IntStream.rangeClosed(1, 10).forEach(n -> questions.add(question("contract-" + n, CONTRACT, 1)));
        List<CardState> reviewed = new ArrayList<>();
        IntStream.rangeClosed(1, 7).forEach(n -> reviewed.add(reviewedToday(String.format("oop-%02d", n))));
        IntStream.rangeClosed(1, 7).forEach(n -> reviewed.add(reviewedToday("types-" + n)));
        PlayerProfile profile = profileWith(Set.of(TYPES, OOP), reviewed.toArray(CardState[]::new));
        ReviewSession session = start(profile, questions, new SessionLimits(0, 1));

        assertThat(session.current()).get().extracting(Question::id).isEqualTo("oop-08");
        AnswerResult result = session.answer(Set.of(1), Confidence.HIGH);

        assertThat(result.masteryBefore()).isEqualTo(7.0 / 13);
        assertThat(result.masteryAfter()).isEqualTo(8.0 / 13);
        assertThat(result.topicsUnlocked()).containsExactly(CONTRACT);
        assertThat(session.profile().unlockedTopics()).containsExactlyInAnyOrder(TYPES, OOP, CONTRACT);
    }

    @Test
    void alreadyUnlockedTopicIsNotReportedAgain() {
        PlayerProfile profile = PlayerProfile.empty(Set.of(TYPES, OOP, CONTRACT));
        ReviewSession session = start(profile, List.of(question("t-1", TYPES, 1)), SessionLimits.DEFAULT);

        assertThat(session.answer(Set.of(1), Confidence.HIGH).topicsUnlocked()).isEmpty();
    }

    @Test
    void answeringTwiceIsRejected() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)), List.of(question("t-1", TYPES, 1)), SessionLimits.DEFAULT);
        session.answer(Set.of(1), Confidence.HIGH);

        assertThatThrownBy(() -> session.answer(Set.of(1), Confidence.HIGH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("question already answered, call next()");
        assertThatThrownBy(() -> session.answerTrueFalse(true, Confidence.HIGH))
                .hasMessage("question already answered, call next()");
        assertThat(session.progress().done()).isEqualTo(1);
    }

    @Test
    void nextBeforeAnsweringIsRejected() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)), List.of(question("t-1", TYPES, 1)), SessionLimits.DEFAULT);

        assertThatThrownBy(session::next)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("question not answered yet, call answer()");
    }

    @Test
    void sessionWalksThroughTheQueueAndFinishes() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)),
                List.of(question("t-1", TYPES, 1), question("t-2", TYPES, 2)),
                SessionLimits.DEFAULT);

        assertThat(session.isFinished()).isFalse();
        session.answer(Set.of(1), Confidence.HIGH);
        assertThat(session.next()).get().extracting(Question::id).isEqualTo("t-2");
        assertThat(session.current()).get().extracting(Question::id).isEqualTo("t-2");
        session.answer(Set.of(1), Confidence.LOW);
        assertThat(session.isFinished()).isFalse();

        assertThat(session.next()).isEmpty();
        assertThat(session.isFinished()).isTrue();
        assertThat(session.current()).isEmpty();
        assertThat(session.next()).isEmpty();
        assertThat(session.progress()).isEqualTo(new SessionProgress(2, 2));
        assertThatThrownBy(() -> session.answer(Set.of(1), Confidence.HIGH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("no current question");
    }

    @Test
    void sessionWithNothingToDoIsFinishedFromTheStart() {
        ReviewSession session = start(PlayerProfile.empty(Set.of(TYPES)), List.of(), SessionLimits.DEFAULT);

        assertThat(session.isFinished()).isTrue();
        assertThat(session.current()).isEmpty();
        assertThat(session.next()).isEmpty();
        assertThat(session.progress()).isEqualTo(new SessionProgress(0, 0));
    }

    @Test
    void startingAgainTheSameDayRebuildsTheQueueFromTheProfile() {
        List<Question> questions = List.of(question("t-1", TYPES, 1), question("t-2", TYPES, 2));
        ReviewSession first = start(PlayerProfile.empty(Set.of(TYPES)), questions, SessionLimits.DEFAULT);
        first.answer(Set.of(1), Confidence.HIGH);

        assertThat(queueIds(first.profile(), questions, SessionLimits.DEFAULT)).containsExactly("t-2");
    }

    @Test
    void markTooEasyReschedulesTheCardAsEasy() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)), List.of(question("t-1", TYPES, 1)), SessionLimits.DEFAULT);
        session.answer(Set.of(1), Confidence.HIGH);
        double goodEase = session.profile().cards().get("t-1").easeFactor();

        AnswerResult result = session.markTooEasy();

        assertThat(result.correct()).isTrue();
        assertThat(session.profile().cards().get("t-1").easeFactor()).isGreaterThan(goodEase);
        assertThat(session.progress().done()).isEqualTo(1);
    }

    @Test
    void markTooEasyKeepsTheUnlockReportOfTheAnswer() {
        List<Question> questions = List.of(
                question("t-1", TYPES, 1), question("o-1", OOP, 1), question("c-1", CONTRACT, 1));
        PlayerProfile profile = profileWith(Set.of(TYPES, OOP), reviewedToday("t-1"));
        ReviewSession session = start(profile, questions, SessionLimits.DEFAULT);

        AnswerResult answered = session.answer(Set.of(1), Confidence.HIGH);
        AnswerResult easy = session.markTooEasy();

        assertThat(answered.topicsUnlocked()).containsExactly(CONTRACT);
        assertThat(easy.topicsUnlocked()).containsExactly(CONTRACT);
        assertThat(session.profile().unlockedTopics()).contains(CONTRACT);
    }

    @Test
    void markTooEasyIsRejectedBeforeAnswerAfterWrongAnswerAndAfterNext() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)),
                List.of(question("t-1", TYPES, 1), question("t-2", TYPES, 2)),
                SessionLimits.DEFAULT);

        assertThatThrownBy(session::markTooEasy).hasMessage("no answered question to mark");
        session.answer(Set.of(0), Confidence.LOW);
        assertThatThrownBy(session::markTooEasy).hasMessage("only a correct answer can be marked too easy");
        session.next();
        assertThatThrownBy(session::markTooEasy).hasMessage("no answered question to mark");
    }

    @Test
    void failedAnswerLeavesTheSessionUntouched() {
        CardState broken = dueCard("broken", TODAY.plusDays(3)).toBuilder().intervalDays(0).build();
        PlayerProfile profile = profileWith(Set.of(TYPES), broken);
        ReviewSession session = start(profile, List.of(question("t-1", TYPES, 1), question("broken", TYPES, 1)),
                SessionLimits.DEFAULT);

        assertThatThrownBy(() -> session.answer(Set.of(1), Confidence.HIGH))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(session.progress().done()).isZero();
        assertThat(session.profile()).isSameAs(profile);
        assertThatThrownBy(session::next).hasMessage("question not answered yet, call answer()");
        assertThatThrownBy(() -> session.answer(Set.of(1), Confidence.HIGH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markTooEasyTwiceGivesTheSameCardAndProfile() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)), List.of(question("t-1", TYPES, 1)), SessionLimits.DEFAULT);
        session.answer(Set.of(1), Confidence.HIGH);
        session.markTooEasy();
        PlayerProfile afterFirst = session.profile();

        session.markTooEasy();

        assertThat(session.profile()).isEqualTo(afterFirst);
    }

    @Test
    void duplicateQuestionIdIsRejected() {
        List<Question> questions = List.of(question("t-1", TYPES, 1), question("t-1", TYPES, 2));

        assertThatThrownBy(() -> start(PlayerProfile.empty(Set.of(TYPES)), questions, SessionLimits.DEFAULT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duplicate question id: t-1");
    }

    @Test
    void negativeLimitIsRejected() {
        assertThatThrownBy(() -> start(PlayerProfile.empty(Set.of(TYPES)), List.of(), new SessionLimits(-1, 5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startWithFiveThousandCardsIsFast() {
        List<Question> questions = IntStream.range(0, 1000)
                .mapToObj(n -> question(String.format("q-%04d", n), TYPES, n % 3))
                .toList();
        Map<String, CardState> cards = new HashMap<>();
        IntStream.range(0, 5000).forEach(n -> {
            String id = n < 1000 ? String.format("q-%04d", n) : "gone-" + n;
            cards.put(id, dueCard(id, TODAY.minusDays(n % 30)));
        });
        PlayerProfile profile = PlayerProfile.empty(Set.of(TYPES)).toBuilder().cards(cards).build();
        start(profile, questions, SessionLimits.DEFAULT);

        long startedAt = System.nanoTime();
        ReviewSession session = start(profile, questions, SessionLimits.DEFAULT);
        long millis = (System.nanoTime() - startedAt) / 1_000_000;

        assertThat(session.progress().total()).isEqualTo(15);
        assertThat(millis).isLessThan(SLOW_CI_LIMIT_MILLIS);
    }

    @Test
    void newQuestionsSeenEarlierTodayUseUpTheDailyLimit() {
        List<Question> questions = IntStream.rangeClosed(1, 8)
                .mapToObj(n -> question("q-" + n, TYPES, 1)).toList();
        PlayerProfile profile = profileWith(Set.of(TYPES), firstSeenToday("q-1"), firstSeenToday("q-2"),
                firstSeenToday("q-3"));

        assertThat(queueIds(profile, questions, SessionLimits.DEFAULT)).containsExactly("q-4", "q-5");
    }

    @Test
    void dailyNewLimitReachedMeansEmptyQueue() {
        List<Question> questions = IntStream.rangeClosed(1, 8)
                .mapToObj(n -> question("q-" + n, TYPES, 1)).toList();
        PlayerProfile profile = profileWith(Set.of(TYPES), firstSeenToday("q-1"), firstSeenToday("q-2"),
                firstSeenToday("q-3"), firstSeenToday("q-4"), firstSeenToday("q-5"), firstSeenToday("q-6"));

        assertThat(start(profile, questions, SessionLimits.DEFAULT).isFinished()).isTrue();
    }

    @Test
    void cardsSeenOnEarlierDaysDoNotCountAgainstTodaysNewLimit() {
        List<Question> questions = IntStream.rangeClosed(1, 8)
                .mapToObj(n -> question("q-" + n, TYPES, 1)).toList();
        CardState earlier = reviewedToday("q-1").toBuilder().firstSeenDate(TODAY.minusDays(3)).build();

        assertThat(queueIds(profileWith(Set.of(TYPES), earlier), questions, SessionLimits.DEFAULT))
                .containsExactly("q-2", "q-3", "q-4", "q-5", "q-6");
    }

    @Test
    void answeringNewQuestionsInTwoRunsStopsAtTheDailyLimit() {
        List<Question> questions = IntStream.rangeClosed(1, 8)
                .mapToObj(n -> question("q-" + n, TYPES, 1)).toList();
        ReviewSession first = start(PlayerProfile.empty(Set.of(TYPES)), questions, SessionLimits.DEFAULT);
        while (first.current().isPresent()) {
            first.answer(Set.of(1), Confidence.HIGH);
            first.next();
        }

        assertThat(start(first.profile(), questions, SessionLimits.DEFAULT).isFinished()).isTrue();
    }

    @Test
    void cardWithoutFirstSeenDateReviewedTodayDoesNotUseUpTheDailyLimit() {
        List<Question> questions = IntStream.rangeClosed(1, 8)
                .mapToObj(n -> question("q-" + n, TYPES, 1)).toList();
        CardState legacy = reviewedToday("q-1");

        assertThat(legacy.firstSeenDate()).isNull();
        assertThat(queueIds(profileWith(Set.of(TYPES), legacy), questions, SessionLimits.DEFAULT))
                .containsExactly("q-2", "q-3", "q-4", "q-5", "q-6");
    }

    @Test
    void answersGiveXpByDifficultyAndWrongOnesGiveTwo() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)),
                List.of(question("t-1", TYPES, 2), question("t-2", TYPES, 2), question("t-3", TYPES, 2)),
                SessionLimits.DEFAULT);

        AnswerResult right = session.answer(Set.of(1), Confidence.HIGH);
        assertThat(right.xpGained()).isEqualTo(20);
        assertThat(right.questCompleted()).isFalse();
        session.next();
        AnswerResult wrong = session.answer(Set.of(0), Confidence.HIGH);

        assertThat(wrong.xpGained()).isEqualTo(2);
        assertThat(session.profile().xp()).isEqualTo(22);
    }

    @Test
    void lastAnswerOfTheQueueCompletesTheQuestAndAddsTheBonus() {
        ReviewSession session = startOnNov2(PlayerProfile.empty(Set.of(TYPES)), twoQuestions());

        session.answer(Set.of(1), Confidence.HIGH);
        session.next();
        AnswerResult last = session.answer(Set.of(0), Confidence.HIGH);

        assertThat(last.questCompleted()).isTrue();
        assertThat(last.xpGained()).isEqualTo(52);
        assertThat(session.profile().xp()).isEqualTo(72);
        assertThat(session.profile().streak()).isEqualTo(new Streak(1, 1, NOV_2));
    }

    @Test
    void secondFullQueueOnTheSameDayGivesNoSecondBonus() {
        List<Question> questions = IntStream.rangeClosed(1, 4)
                .mapToObj(n -> question("q-" + n, TYPES, 1)).toList();
        SessionLimits oneAtATime = new SessionLimits(0, 1);
        ReviewSession first = ReviewSession.start(PlayerProfile.empty(Set.of(TYPES)),
                new ContentPack("test", questions), topics, TODAY, oneAtATime);
        first.answer(Set.of(1), Confidence.HIGH);
        SessionLimits oneMore = new SessionLimits(0, 2);

        ReviewSession second = ReviewSession.start(first.profile(),
                new ContentPack("test", questions), topics, TODAY, oneMore);
        AnswerResult result = second.answer(Set.of(1), Confidence.HIGH);

        assertThat(result.questCompleted()).isFalse();
        assertThat(result.xpGained()).isEqualTo(10);
        assertThat(second.profile().xp()).isEqualTo(70);
        assertThat(second.profile().streak()).isEqualTo(new Streak(1, 1, TODAY));
    }

    @Test
    void markingTheLastAnswerTooEasyDoesNotPayTheBonusTwice() {
        ReviewSession session = start(
                PlayerProfile.empty(Set.of(TYPES)), List.of(question("t-1", TYPES, 2)), SessionLimits.DEFAULT);
        session.answer(Set.of(1), Confidence.HIGH);

        AnswerResult easy = session.markTooEasy();

        assertThat(easy.xpGained()).isEqualTo(70);
        assertThat(easy.questCompleted()).isTrue();
        assertThat(session.profile().xp()).isEqualTo(70);
        assertThat(session.profile().streak()).isEqualTo(new Streak(1, 1, TODAY));
    }

    @Test
    void lastAnswerWithTheClockMovedBackGivesNoBonusAndKeepsTheStreak() {
        Streak streak = new Streak(5, 5, NOV_2);
        PlayerProfile profile = PlayerProfile.empty(Set.of(TYPES)).toBuilder().streak(streak).build();
        ReviewSession session = start(profile, List.of(question("t-1", TYPES, 1)), SessionLimits.DEFAULT);

        AnswerResult last = session.answer(Set.of(1), Confidence.HIGH);

        assertThat(last.questCompleted()).isFalse();
        assertThat(session.profile().xp()).isEqualTo(10);
        assertThat(session.profile().streak()).isEqualTo(streak);
    }

    @Test
    void emptyQueueCountsTheQuestAsDoneOnce() {
        PlayerProfile profile = PlayerProfile.empty(Set.of(TYPES)).toBuilder()
                .streak(new Streak(4, 4, NOV_2.minusDays(1)))
                .build();

        ReviewSession session = startOnNov2(profile, List.of());

        assertThat(session.questCompletedAtStart()).isTrue();
        assertThat(session.profile().xp()).isEqualTo(50);
        assertThat(session.profile().streak()).isEqualTo(new Streak(5, 5, NOV_2));
        ReviewSession restarted = startOnNov2(session.profile(), List.of());
        assertThat(restarted.questCompletedAtStart()).isFalse();
        assertThat(restarted.profile().xp()).isEqualTo(50);
    }

    @Test
    void sessionWithQuestionsDoesNotCompleteTheQuestAtStart() {
        ReviewSession session = startOnNov2(PlayerProfile.empty(Set.of(TYPES)), twoQuestions());

        assertThat(session.questCompletedAtStart()).isFalse();
        assertThat(session.profile().xp()).isZero();
        assertThat(session.profile().streak()).isEqualTo(Streak.none());
    }

    private List<Question> twoQuestions() {
        return List.of(question("t-1", TYPES, 2), question("t-2", TYPES, 2));
    }

    private ReviewSession startOnNov2(PlayerProfile profile, List<Question> questions) {
        return ReviewSession.start(profile, new ContentPack("test", questions), topics, NOV_2, SessionLimits.DEFAULT);
    }

    private static CardState firstSeenToday(String id) {
        return reviewedToday(id).toBuilder().firstSeenDate(TODAY).build();
    }

    private ReviewSession start(PlayerProfile profile, List<Question> questions, SessionLimits limits) {
        return ReviewSession.start(profile, new ContentPack("test", questions), topics, TODAY, limits);
    }

    private List<String> queueIds(PlayerProfile profile, List<Question> questions, SessionLimits limits) {
        ReviewSession session = start(profile, questions, limits);
        List<String> ids = new ArrayList<>();
        session.current().ifPresent(first -> ids.add(first.id()));
        while (ids.size() < session.progress().total()) {
            session.answer(Set.of(1), Confidence.HIGH);
            ids.add(session.next().orElseThrow().id());
        }
        return ids;
    }

    private static Topic topic(String id, String... prerequisites) {
        return Topic.builder()
                .id(id).title(id).islandId("core").prerequisites(List.of(prerequisites))
                .minJavaVersion(8).keyPoints(List.of())
                .build();
    }

    private static Question question(String id, String topic, int difficulty) {
        return Question.builder()
                .id(id).topic(topic).type(QuestionType.SINGLE_CHOICE).difficulty(difficulty)
                .prompt("prompt " + id)
                .options(List.of(new TextOption("a"), new TextOption("b")))
                .correct(new SingleIndex(1))
                .explanation("because " + id).reference("JLS 1").minJavaVersion(8).source("test")
                .build();
    }

    private static CardState dueCard(String id, LocalDate due) {
        return CardState.builder()
                .cardId(id).repetitions(1).easeFactor(2.5).intervalDays(1)
                .dueDate(due).lastReviewDate(due.minusDays(1))
                .build();
    }

    private static CardState reviewedToday(String id) {
        return CardState.builder()
                .cardId(id).repetitions(1).easeFactor(2.5).intervalDays(1)
                .dueDate(TODAY.plusDays(1)).lastReviewDate(TODAY)
                .build();
    }

    private static PlayerProfile profileWith(Set<String> unlocked, CardState... cards) {
        Map<String, CardState> byId = new HashMap<>();
        for (CardState card : cards) {
            byId.put(card.cardId(), card);
        }
        return PlayerProfile.empty(unlocked).toBuilder().cards(byId).build();
    }
}
