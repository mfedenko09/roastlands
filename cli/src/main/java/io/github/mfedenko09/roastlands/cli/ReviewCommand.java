package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.answer.Confidence;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.profile.LoadResult;
import io.github.mfedenko09.roastlands.core.profile.PlayerProfile;
import io.github.mfedenko09.roastlands.core.profile.ProfileStore;
import io.github.mfedenko09.roastlands.core.session.AnswerResult;
import io.github.mfedenko09.roastlands.core.session.ReviewSession;
import io.github.mfedenko09.roastlands.core.session.SessionLimits;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

final class ReviewCommand {

    private static final String RESTORED_WARNING = "Warning: the profile file was damaged, progress is restored from "
            + "the backup.";
    private static final String CONFIRM_PROMPT = "Sure? (y/n): ";
    private static final String CONFIRM_HINT = "Please type y or n.";
    private static final String INPUT_CLOSED = "Input closed. Answered questions are saved.";

    private final GameContent content;
    private final ProfileStore store;
    private final Terminal terminal;
    private final LocalDate today;
    private final AnswerParser answerParser = new AnswerParser();
    private final QuestionFormatter questionFormatter = new QuestionFormatter();
    private final FeedbackFormatter feedbackFormatter = new FeedbackFormatter();
    private final NextReview nextReview = new NextReview();

    ReviewCommand(GameContent content, ProfileStore store, Terminal terminal, LocalDate today) {
        this.content = content;
        this.store = store;
        this.terminal = terminal;
        this.today = today;
    }

    int run() {
        LoadResult loaded = store.load();
        if (loaded.restoredFromBackup()) {
            terminal.println(RESTORED_WARNING);
        }
        ReviewSession session = ReviewSession.start(
                loaded.profile(), content.pack(), content.topics(), today, SessionLimits.DEFAULT);
        if (session.isFinished()) {
            terminal.println("Nothing due today." + nextReviewSuffix(session.profile()));
            return 0;
        }
        int right = 0;
        while (session.current().isPresent()) {
            Question question = session.current().get();
            terminal.println(questionFormatter.format(question, session.progress()));
            Optional<AnswerResult> result = answer(session, question);
            if (result.isEmpty()) {
                terminal.println(INPUT_CLOSED);
                return 0;
            }
            store.save(session.profile());
            terminal.println(feedbackFormatter.format(question, result.get(), terminal.colors()));
            right += result.get().correct() ? 1 : 0;
            session.next();
        }
        terminal.println("");
        terminal.println("Done: " + right + " of " + session.progress().total() + " right."
                + nextReviewSuffix(session.profile()));
        return 0;
    }

    private Optional<AnswerResult> answer(ReviewSession session, Question question) {
        String prompt = questionFormatter.promptFor(question);
        String hint = questionFormatter.hintFor(question);
        if (question.type() == QuestionType.TRUE_FALSE) {
            return terminal.ask(prompt, answerParser::parseTrueFalse, hint)
                    .flatMap(chosen -> askConfidence().map(confidence -> session.answerTrueFalse(chosen, confidence)));
        }
        int optionCount = question.options().size();
        Function<String, Optional<Set<Integer>>> parser = question.type() == QuestionType.MULTI_CHOICE
                ? input -> answerParser.parseMulti(input, optionCount)
                : input -> answerParser.parseSingle(input, optionCount);
        return terminal.ask(prompt, parser, hint)
                .flatMap(chosen -> askConfidence().map(confidence -> session.answer(chosen, confidence)));
    }

    private Optional<Confidence> askConfidence() {
        return terminal.ask(CONFIRM_PROMPT, answerParser::parseSure, CONFIRM_HINT)
                .map(sure -> sure ? Confidence.HIGH : Confidence.LOW);
    }

    private String nextReviewSuffix(PlayerProfile profile) {
        return nextReview.after(profile.cards().values(), content.pack().questions(), today)
                .map(date -> " Next review: " + date)
                .orElse("");
    }
}
