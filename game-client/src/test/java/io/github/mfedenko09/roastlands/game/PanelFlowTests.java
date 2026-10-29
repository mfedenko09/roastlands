package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.mfedenko09.roastlands.core.answer.Confidence;
import io.github.mfedenko09.roastlands.core.content.InvalidContentException;
import io.github.mfedenko09.roastlands.core.profile.ProfileCorruptedException;
import io.github.mfedenko09.roastlands.core.profile.ProfileStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PanelFlowTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final String PACK = "id: demo\ntitle: Demo\nversion: 1.0.0\nminJavaVersion: 21\n";
    private static final String TOPICS = """
            islands:
              - id: shore
                title: Shore
            topics:
              - id: basics
                title: Basics
                island: shore
            """;
    private static final String QUESTIONS = """
            - id: basics.tf
              topic: basics
              type: TRUE_FALSE
              difficulty: 1
              prompt: Is Java pass-by-value?
              correct: true
              explanation: Java copies the value of the argument.
              reference: JLS §15.12.4.5
            - id: basics.single
              topic: basics
              type: SINGLE_CHOICE
              difficulty: 2
              prompt: Which one?
              options: [first, second, third]
              correct: 1
              explanation: Second is right.
              reference: JLS §1.1
            - id: basics.multi
              topic: basics
              type: MULTI_CHOICE
              difficulty: 3
              prompt: Which ones?
              options: [first, second, third]
              correct: [0, 2]
              explanation: First and third.
              reference: JLS §1.2
            """;

    @TempDir
    Path root;

    private GamePaths paths;

    @BeforeEach
    void writeContent() throws IOException {
        Path pack = root.resolve("content").resolve("packs").resolve("demo");
        Files.createDirectories(pack.resolve("questions"));
        Files.writeString(pack.resolve("pack.yaml"), PACK);
        Files.writeString(pack.resolve("topics.yaml"), TOPICS);
        Files.writeString(pack.resolve("questions").resolve("basics.yaml"), QUESTIONS);
        paths = new GamePaths(root.resolve("content"), root.resolve("profile"));
    }

    @Test
    void startsOnTheEasiestQuestionWithHeaderAndLabels() {
        PanelFlow flow = GameStartup.open(paths, TODAY);

        assertThat(flow.step()).isEqualTo(PanelStep.CHOOSING);
        assertThat(flow.header()).isEqualTo("Basics  |  Difficulty 1  |  0/3");
        assertThat(flow.optionLabels()).containsExactly("True", "False");
        assertThat(flow.notice()).isEmpty();
    }

    @Test
    void singleChoicePickGoesToConfidenceAndAnswerSavesBeforeTheResultStep() {
        PanelFlow flow = openAtSingleChoice();

        flow.pick(1);
        assertThat(flow.step()).isEqualTo(PanelStep.CONFIDENCE);
        flow.pick(2);
        flow.pick(1);
        flow.answer(Confidence.HIGH);

        assertThat(flow.step()).isEqualTo(PanelStep.RESULT);
        assertThat(flow.result().correct()).isTrue();
        assertThat(flow.correctIndexes()).containsExactly(1);
        assertThat(flow.saveError()).isEmpty();
        assertThat(savedCards()).contains("basics.single");
    }

    @Test
    void wrongTrueFalseAnswerIsReportedWithTheRightOption() {
        PanelFlow flow = GameStartup.open(paths, TODAY);

        flow.pick(1);
        flow.answer(Confidence.LOW);

        assertThat(flow.result().correct()).isFalse();
        assertThat(flow.chosen()).containsExactly(1);
        assertThat(flow.correctIndexes()).containsExactly(0);
        assertThat(flow.canMarkTooEasy()).isFalse();
    }

    @Test
    void multiChoiceNeedsSubmitAndIgnoresPicksAfterIt() {
        PanelFlow flow = openAtMultiChoice();

        flow.confirm();
        assertThat(flow.step()).isEqualTo(PanelStep.CHOOSING);
        flow.pick(0);
        flow.pick(2);
        flow.pick(1);
        flow.pick(1);
        assertThat(flow.isMultiChoice()).isTrue();
        assertThat(flow.step()).isEqualTo(PanelStep.CHOOSING);
        flow.confirm();
        assertThat(flow.step()).isEqualTo(PanelStep.CONFIDENCE);
        flow.pick(1);
        assertThat(flow.chosen()).containsExactly(0, 2);
        flow.answer(Confidence.HIGH);

        assertThat(flow.result().correct()).isTrue();
        assertThat(flow.correctIndexes()).containsExactlyInAnyOrder(0, 2);
    }

    @Test
    void answeringOutOfOrderChangesNothing() {
        PanelFlow flow = GameStartup.open(paths, TODAY);

        flow.answer(Confidence.HIGH);
        flow.confirm();
        flow.markTooEasy();

        assertThat(flow.step()).isEqualTo(PanelStep.CHOOSING);
        assertThat(flow.summary().answered()).isZero();
    }

    @Test
    void tooEasyRaisesTheEaseOnDiskAndCanBeUsedOnce() {
        PanelFlow flow = openAtSingleChoice();
        flow.pick(1);
        flow.answer(Confidence.HIGH);
        double goodEase = savedEase("basics.single");

        assertThat(flow.canMarkTooEasy()).isTrue();
        flow.markTooEasy();

        assertThat(savedEase("basics.single")).isGreaterThan(goodEase);
        assertThat(flow.canMarkTooEasy()).isFalse();
        double easyEase = savedEase("basics.single");
        flow.markTooEasy();
        assertThat(savedEase("basics.single")).isEqualTo(easyEase);
    }

    @Test
    void failedSaveShowsReasonAndTheSessionGoesOn() throws IOException {
        Path blocker = Files.writeString(root.resolve("blocker"), "a file, not a folder");
        PanelFlow flow = GameStartup.open(new GamePaths(paths.contentDir(), blocker.resolve("profile")), TODAY);

        flow.pick(0);
        flow.answer(Confidence.HIGH);

        assertThat(flow.step()).isEqualTo(PanelStep.RESULT);
        assertThat(flow.saveError()).hasValueSatisfying(error -> assertThat(error).startsWith("Progress not saved: "));
        flow.confirm();
        assertThat(flow.step()).isEqualTo(PanelStep.CHOOSING);
        assertThat(flow.question().id()).isEqualTo("basics.single");
    }

    @Test
    void endOfDayShowsAnsweredCorrectPercentAndNoUnlocks() {
        PanelFlow flow = GameStartup.open(paths, TODAY);
        flow.pick(0);
        flow.answer(Confidence.HIGH);
        flow.confirm();
        flow.pick(0);
        flow.answer(Confidence.HIGH);
        flow.confirm();
        flow.pick(0);
        flow.pick(2);
        flow.confirm();
        flow.answer(Confidence.HIGH);
        flow.confirm();

        assertThat(flow.step()).isEqualTo(PanelStep.DONE);
        assertThat(flow.summary()).isEqualTo(new DaySummary(3, 2, 67, List.of()));
    }

    @Test
    void closingAndReopeningTheGameContinuesFromTheNextQuestion() {
        PanelFlow first = GameStartup.open(paths, TODAY);
        first.pick(0);
        first.answer(Confidence.HIGH);

        PanelFlow second = GameStartup.open(paths, TODAY);

        assertThat(second.question().id()).isEqualTo("basics.single");
        assertThat(second.header()).isEqualTo("Basics  |  Difficulty 2  |  0/2");
    }

    @Test
    void nothingDueAfterAFullDayGoesStraightToTheSummary() {
        answerEverythingToday();

        PanelFlow flow = GameStartup.open(paths, TODAY);

        assertThat(flow.step()).isEqualTo(PanelStep.DONE);
        assertThat(flow.summary().answered()).isZero();
    }

    @Test
    void damagedProfileIsRestoredFromBackupWithANotice() throws IOException {
        answerEverythingToday();
        Files.writeString(paths.profileDir().resolve("profile.json"), "{ not json");

        PanelFlow flow = GameStartup.open(paths, TODAY);

        assertThat(flow.notice()).hasValue(PanelFlow.RESTORED_NOTICE);
    }

    @Test
    void unlockedTopicIdsAreShownAsTitlesAndUnknownIdsStay() {
        PanelFlow flow = GameStartup.open(paths, TODAY);

        assertThat(flow.titles(List.of("basics", "ghost"))).containsExactly("Basics", "ghost");
    }

    @Test
    void optionMarksFollowTheStep() {
        PanelFlow flow = openAtSingleChoice();

        assertThat(flow.canPick()).isTrue();
        assertThat(marks(flow)).containsExactly(OptionMark.NEUTRAL, OptionMark.NEUTRAL, OptionMark.NEUTRAL);
        flow.pick(2);
        assertThat(flow.canPick()).isTrue();
        assertThat(marks(flow)).containsExactly(OptionMark.NEUTRAL, OptionMark.NEUTRAL, OptionMark.CHOSEN);
        flow.answer(Confidence.HIGH);
        assertThat(flow.canPick()).isFalse();
        assertThat(marks(flow)).containsExactly(OptionMark.NEUTRAL, OptionMark.CORRECT, OptionMark.WRONG_CHOSEN);
    }

    @Test
    void picksAreLockedOnMultiChoiceAfterSubmit() {
        PanelFlow flow = openAtMultiChoice();
        flow.pick(0);
        assertThat(flow.canPick()).isTrue();
        flow.confirm();

        assertThat(flow.canPick()).isFalse();
        assertThat(marks(flow)).containsExactly(OptionMark.CHOSEN, OptionMark.NEUTRAL, OptionMark.NEUTRAL);
    }

    @Test
    void brokenContentFailsTheStartupWithAReadableReason() throws IOException {
        Files.writeString(paths.contentDir().resolve("packs/demo/questions/basics.yaml"), "- id: broken\n");

        InvalidContentException error = catchThrowableOfType(
                InvalidContentException.class, () -> GameStartup.open(paths, TODAY));

        assertThat(ErrorReason.of(error)).isNotBlank();
    }

    @Test
    void bothProfileFilesBrokenFailsTheStartup() throws IOException {
        answerEverythingToday();
        Files.writeString(paths.profileDir().resolve("profile.json"), "{ not json");
        Files.writeString(paths.profileDir().resolve("profile.json.bak"), "{ not json");

        assertThatThrownBy(() -> GameStartup.open(paths, TODAY)).isInstanceOf(ProfileCorruptedException.class);
    }

    @Test
    void tooEasyKeepsTheTopicsUnlockedByTheAnswer() throws IOException {
        Files.writeString(paths.contentDir().resolve("packs").resolve("demo").resolve("topics.yaml"), TOPICS
                + "  - id: deep\n    title: Deep\n    island: shore\n    prerequisites: [basics]\n");
        PanelFlow flow = openAtMultiChoice();
        flow.pick(0);
        flow.pick(2);
        flow.confirm();
        flow.answer(Confidence.HIGH);
        List<String> unlocked = flow.summary().topicsUnlocked();

        flow.markTooEasy();

        assertThat(unlocked).containsExactly("deep");
        assertThat(flow.summary().topicsUnlocked()).containsExactly("deep");
        assertThat(flow.summary().answered()).isEqualTo(3);
    }

    private static List<OptionMark> marks(PanelFlow flow) {
        return IntStream.range(0, flow.optionLabels().size()).mapToObj(flow::markOf).toList();
    }

    private PanelFlow openAtSingleChoice() {
        PanelFlow flow = GameStartup.open(paths, TODAY);
        flow.pick(0);
        flow.answer(Confidence.HIGH);
        flow.confirm();
        return flow;
    }

    private PanelFlow openAtMultiChoice() {
        PanelFlow flow = openAtSingleChoice();
        flow.pick(1);
        flow.answer(Confidence.HIGH);
        flow.confirm();
        return flow;
    }

    private void answerEverythingToday() {
        PanelFlow flow = openAtMultiChoice();
        flow.pick(0);
        flow.confirm();
        flow.answer(Confidence.LOW);
        flow.confirm();
    }

    private Set<String> savedCards() {
        return new ProfileStore(paths.profileDir(), Set.of("basics")).load().profile().cards().keySet();
    }

    private double savedEase(String cardId) {
        return new ProfileStore(paths.profileDir(), Set.of("basics")).load().profile().cards().get(cardId)
                .easeFactor();
    }
}
