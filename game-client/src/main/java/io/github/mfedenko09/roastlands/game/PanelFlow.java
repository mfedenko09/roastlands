package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.answer.Confidence;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.profile.ProfileStore;
import io.github.mfedenko09.roastlands.core.session.AnswerResult;
import io.github.mfedenko09.roastlands.core.session.ReviewSession;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class PanelFlow {

    static final String RESTORED_NOTICE = "The profile file was damaged, progress is restored from the backup.";

    private static final String NOT_SAVED = "Progress not saved: ";
    private static final int TRUE_FALSE_OPTIONS = 2;

    private final ReviewSession session;
    private final ProfileStore store;
    private final Map<String, String> topicTitles;
    private final String notice;
    private final DayTally tally = new DayTally();

    private PanelStep step;
    private Question question;
    private AnswerDraft draft;
    private AnswerResult result;
    private String saveError;
    private boolean markedTooEasy;

    PanelFlow(ReviewSession session, ProfileStore store, TopicGraph topics, boolean restoredFromBackup) {
        this.session = session;
        this.store = store;
        this.topicTitles = topics.topics().stream().collect(Collectors.toMap(Topic::id, Topic::title));
        this.notice = restoredFromBackup ? RESTORED_NOTICE : null;
        showCurrent();
    }

    PanelStep step() {
        return step;
    }

    Question question() {
        return question;
    }

    String header() {
        return PanelHeader.text(topicTitles.get(question.topic()), question.difficulty(), session.progress());
    }

    List<String> optionLabels() {
        return OptionLabels.forQuestion(question);
    }

    boolean isMultiChoice() {
        return draft.isMultiChoice();
    }

    Set<Integer> chosen() {
        return draft.selected();
    }

    AnswerResult result() {
        return result;
    }

    Set<Integer> correctIndexes() {
        return CorrectIndexes.of(result.correctAnswer());
    }

    OptionMark markOf(int index) {
        boolean chosen = draft.selected().contains(index);
        if (step == PanelStep.RESULT) {
            if (correctIndexes().contains(index)) {
                return OptionMark.CORRECT;
            }
            return chosen ? OptionMark.WRONG_CHOSEN : OptionMark.NEUTRAL;
        }
        return chosen ? OptionMark.CHOSEN : OptionMark.NEUTRAL;
    }

    boolean canPick() {
        return step == PanelStep.CHOOSING || step == PanelStep.CONFIDENCE && !draft.isMultiChoice();
    }

    boolean canMarkTooEasy() {
        return step == PanelStep.RESULT && result.correct() && !markedTooEasy;
    }

    Optional<String> saveError() {
        return Optional.ofNullable(saveError);
    }

    Optional<String> notice() {
        return Optional.ofNullable(notice);
    }

    DaySummary summary() {
        return tally.summary();
    }

    List<String> titles(List<String> topicIds) {
        return topicIds.stream().map(id -> topicTitles.getOrDefault(id, id)).toList();
    }

    void pick(int index) {
        if (!canPick()) {
            return;
        }
        draft.pick(index);
        if (!draft.isMultiChoice() && !draft.isEmpty()) {
            step = PanelStep.CONFIDENCE;
        }
    }

    void confirm() {
        if (step == PanelStep.CHOOSING && draft.isMultiChoice() && !draft.isEmpty()) {
            step = PanelStep.CONFIDENCE;
        } else if (step == PanelStep.RESULT) {
            session.next();
            showCurrent();
        }
    }

    void answer(Confidence confidence) {
        if (step != PanelStep.CONFIDENCE) {
            return;
        }
        result = question.type() == QuestionType.TRUE_FALSE
                ? session.answerTrueFalse(draft.selected().contains(CorrectIndexes.TRUE_INDEX), confidence)
                : session.answer(draft.selected(), confidence);
        tally.record(result);
        markedTooEasy = false;
        save();
        step = PanelStep.RESULT;
    }

    void markTooEasy() {
        if (!canMarkTooEasy()) {
            return;
        }
        result = session.markTooEasy();
        tally.replaceLast(result);
        markedTooEasy = true;
        save();
    }

    private void showCurrent() {
        Optional<Question> current = session.current();
        question = current.orElse(null);
        step = current.isPresent() ? PanelStep.CHOOSING : PanelStep.DONE;
        current.ifPresent(next -> draft = new AnswerDraft(next.type(), optionCountOf(next)));
    }

    private static int optionCountOf(Question next) {
        return next.type() == QuestionType.TRUE_FALSE ? TRUE_FALSE_OPTIONS : next.options().size();
    }

    private void save() {
        try {
            store.save(session.profile());
            saveError = null;
        } catch (RuntimeException e) {
            saveError = NOT_SAVED + ErrorReason.of(e);
        }
    }
}
