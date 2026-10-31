package io.github.mfedenko09.roastlands.core.session;

import io.github.mfedenko09.roastlands.core.answer.Confidence;
import io.github.mfedenko09.roastlands.core.answer.GradeResolver;
import io.github.mfedenko09.roastlands.core.content.AnswerChecker;
import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.profile.PlayerProfile;
import io.github.mfedenko09.roastlands.core.profile.Streak;
import io.github.mfedenko09.roastlands.core.progress.StreakRules;
import io.github.mfedenko09.roastlands.core.progress.XpRules;
import io.github.mfedenko09.roastlands.core.srs.CardState;
import io.github.mfedenko09.roastlands.core.srs.Grade;
import io.github.mfedenko09.roastlands.core.srs.Sm2Scheduler;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * One day of play: due reviews first, then new questions from the earliest open topics.
 * The queue is fixed at start. The class is not thread-safe, call it from one thread only.
 */
public final class ReviewSession {

    private final Sm2Scheduler scheduler = new Sm2Scheduler();
    private final GradeResolver gradeResolver = new GradeResolver();
    private final AnswerChecker answerChecker = new AnswerChecker();
    private final XpRules xpRules = new XpRules();
    private final StreakRules streakRules = new StreakRules();
    private final TopicGraph topics;
    private final MasteryBoard masteryBoard;
    private final LocalDate today;
    private final List<Question> queue;
    private final boolean questCompletedAtStart;

    private PlayerProfile profile;
    private PlayerProfile profileBeforeAnswer;
    private boolean answeredCorrectly;
    private int position;
    private int done;

    private ReviewSession(
            PlayerProfile profile,
            Collection<Question> questions,
            TopicGraph topics,
            LocalDate today,
            List<Question> queue) {
        this.topics = topics;
        this.today = today;
        this.queue = queue;
        this.masteryBoard = new MasteryBoard(questions);
        this.profile = queue.isEmpty() ? withQuestCompleted(profile) : profile;
        this.questCompletedAtStart = isQuestCompleted(profile, this.profile);
    }

    public static ReviewSession start(
            PlayerProfile profile, ContentPack content, TopicGraph topics, LocalDate today, SessionLimits limits) {
        Map<String, Question> questionsById = content.questions().stream()
                .collect(Collectors.toMap(Question::id, question -> question, ReviewSession::rejectDuplicate));
        List<CardState> knownCards = profile.cards().values().stream()
                .filter(card -> questionsById.containsKey(card.cardId()))
                .toList();
        List<String> newCandidates = new NewQuestionPicker(topics).candidates(content.questions(), profile);
        long newToday = knownCards.stream().filter(card -> today.equals(card.firstSeenDate())).count();
        int newLeftToday = (int) Math.max(0, limits.maxNew() - newToday);
        DailyQueue dailyQueue = new DailyQueueBuilder()
                .build(knownCards, newCandidates, today, limits.maxReviews(), newLeftToday);
        List<Question> queue = Stream.concat(dailyQueue.reviewCardIds().stream(), dailyQueue.newCardIds().stream())
                .map(questionsById::get)
                .toList();
        return new ReviewSession(profile, content.questions(), topics, today, queue);
    }

    public Optional<Question> current() {
        return isFinished() ? Optional.empty() : Optional.of(queue.get(position));
    }

    public Optional<Question> next() {
        if (isFinished()) {
            return Optional.empty();
        }
        if (profileBeforeAnswer == null) {
            throw new IllegalStateException("question not answered yet, call answer()");
        }
        position++;
        profileBeforeAnswer = null;
        return current();
    }

    public boolean isFinished() {
        return position >= queue.size();
    }

    public PlayerProfile profile() {
        return profile;
    }

    public boolean questCompletedAtStart() {
        return questCompletedAtStart;
    }

    public SessionProgress progress() {
        return new SessionProgress(done, queue.size());
    }

    public AnswerResult answer(Set<Integer> chosen, Confidence confidence) {
        Question question = unansweredQuestion();
        return record(question, answerChecker.isCorrect(question, chosen), confidence);
    }

    public AnswerResult answerTrueFalse(boolean chosen, Confidence confidence) {
        Question question = unansweredQuestion();
        return record(question, answerChecker.isCorrect(question, chosen), confidence);
    }

    public AnswerResult markTooEasy() {
        if (profileBeforeAnswer == null) {
            throw new IllegalStateException("no answered question to mark");
        }
        if (!answeredCorrectly) {
            throw new IllegalStateException("only a correct answer can be marked too easy");
        }
        return apply(profileBeforeAnswer, queue.get(position), true, Grade.EASY);
    }

    private static Question rejectDuplicate(Question first, Question second) {
        throw new IllegalArgumentException("duplicate question id: " + first.id());
    }

    private Question unansweredQuestion() {
        if (isFinished()) {
            throw new IllegalStateException("no current question");
        }
        if (profileBeforeAnswer != null) {
            throw new IllegalStateException("question already answered, call next()");
        }
        return queue.get(position);
    }

    private AnswerResult record(Question question, boolean correct, Confidence confidence) {
        Grade grade = gradeResolver.resolve(correct, confidence);
        PlayerProfile base = profile;
        AnswerResult result = apply(base, question, correct, grade);
        profileBeforeAnswer = base;
        answeredCorrectly = correct;
        done++;
        return result;
    }

    private AnswerResult apply(PlayerProfile base, Question question, boolean correct, Grade grade) {
        CardState reviewed = scheduler.review(
                base.cards().getOrDefault(question.id(), CardState.newCard(question.id())), grade, today);
        Map<String, CardState> cards = new HashMap<>(base.cards());
        cards.put(question.id(), reviewed);
        Map<String, Double> masteryBefore = masteryBoard.masteryByTopic(base.cards(), today);
        Map<String, Double> masteryAfter = masteryBoard.masteryByTopic(cards, today);
        Set<String> unlocked = new LinkedHashSet<>(topics.unlockable(masteryAfter, base.unlockedTopics()));
        PlayerProfile answered = base.toBuilder()
                .cards(cards)
                .unlockedTopics(unlocked)
                .xp(base.xp() + xpRules.forAnswer(correct, question.difficulty()))
                .build();
        profile = position == queue.size() - 1 ? withQuestCompleted(answered) : answered;
        return AnswerResult.builder()
                .correct(correct)
                .correctAnswer(question.correct())
                .explanation(question.explanation())
                .reference(question.reference())
                .topicId(question.topic())
                .masteryBefore(masteryBefore.get(question.topic()))
                .masteryAfter(masteryAfter.get(question.topic()))
                .topicsUnlocked(unlocked.stream().filter(id -> !base.unlockedTopics().contains(id)).toList())
                .xpGained(profile.xp() - base.xp())
                .questCompleted(isQuestCompleted(base, profile))
                .build();
    }

    private PlayerProfile withQuestCompleted(PlayerProfile player) {
        Streak streak = streakRules.onQuestCompleted(player.streak(), today);
        if (streak.equals(player.streak())) {
            return player;
        }
        return player.toBuilder().streak(streak).xp(player.xp() + XpRules.QUEST_BONUS).build();
    }

    private static boolean isQuestCompleted(PlayerProfile before, PlayerProfile after) {
        return !before.streak().equals(after.streak());
    }
}
