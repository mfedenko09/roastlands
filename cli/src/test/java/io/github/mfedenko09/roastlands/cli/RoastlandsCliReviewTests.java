package io.github.mfedenko09.roastlands.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.profile.PlayerProfile;
import io.github.mfedenko09.roastlands.core.profile.ProfileStore;
import io.github.mfedenko09.roastlands.core.profile.Streak;
import io.github.mfedenko09.roastlands.core.progress.XpRules;
import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RoastlandsCliReviewTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final String ALL_RIGHT = "t\ny\n2\ny\n1,3\ny\n1\ny\n";

    @TempDir
    Path content;

    @TempDir
    Path profileDir;

    @BeforeEach
    void writeContent() {
        CliTestSupport.writeContent(content);
    }

    @Test
    void dailySessionAsksEachQuestionPrintsFeedbackAndSavesTheProfile() {
        CliResult result = review(ALL_RIGHT);

        assertThat(result.exitCode()).isZero();
        assertThat(result.err()).isEmpty();
        assertThat(result.out()).isEqualTo("""

                Question 1/4 - basics
                True or false - Java is pass-by-value.
                Your answer (t/f): Sure? (y/n): Right!
                Java copies the value of the argument, also for references.
                Reference: JLS §15.12.4.5
                Mastery of basics: 0% -> 25%

                Question 2/4 - basics
                Which keyword stops inheritance?

                  1) abstract
                  2) final
                  3) static
                Your answer (1-3): Sure? (y/n): Right!
                A final class cannot be extended.
                Reference: JLS §8.1.1.2
                Mastery of basics: 25% -> 50%

                Question 3/4 - basics
                Which are primitive types?

                  1) int
                  2) String
                  3) boolean
                  4) Integer
                Your answers, comma separated (1-4): Sure? (y/n): Right!
                String and Integer are classes.
                Reference: JLS §4.2
                Mastery of basics: 50% -> 75%
                Unlocked: deep

                Question 4/4 - basics
                What does this code print?

                public class Main {
                    public static void main(String[] args) {
                        System.out.println(1 + 2 + "3");
                        System.out.println("1" + 2 + 3);
                    }
                }

                  1) 33
                     123
                  2) 123
                     123
                  3) Compile error
                Your answer (1-3): Sure? (y/n): Right!
                Plus is evaluated from left to right.
                Reference: JLS §15.18
                Mastery of basics: 75% -> 100%

                Done: 4 of 4 right. Next review: 2026-10-29
                """);
        PlayerProfile saved = savedProfile();
        assertThat(saved.cards()).containsOnlyKeys(
                "basics.true-false", "basics.single", "basics.multi", "basics.output");
        assertThat(saved.unlockedTopics()).containsExactlyInAnyOrder("basics", "deep");
    }

    @Test
    void wrongAnswerShowsTheCorrectOneAndIsSavedAsALapse() {
        CliResult result = review("f\ny\n");

        assertThat(result.out()).contains("""
                Wrong.
                Correct answer: true
                Java copies the value of the argument, also for references.
                """);
        assertThat(savedProfile().cards().get("basics.true-false").lapses()).isEqualTo(1);
    }

    @Test
    void wrongMultipleChoiceListsAllCorrectOptions() {
        CliResult result = review("t\ny\n2\ny\n1\ny\n");

        assertThat(result.out()).contains("Wrong.\nCorrect answer: 1) int; 3) boolean\n");
    }

    @Test
    void numberOutsideTheOptionsIsAskedAgainAndNotCounted() {
        CliResult result = review("t\ny\n2\ny\n1,3\ny\n7\n");

        assertThat(result.out())
                .contains("Your answer (1-3): Please type one number from 1 to 3.\nYour answer (1-3): ");
        assertThat(savedProfile().cards()).doesNotContainKey("basics.output").hasSize(3);
    }

    @Test
    void unreadableAnswersAndConfirmationsAreAskedAgain() {
        CliResult result = review("maybe\nt\nperhaps\ny\n");

        assertThat(result.out())
                .contains("Your answer (t/f): Please type t or f.\n")
                .contains("Sure? (y/n): Please type y or n.\n");
        assertThat(savedProfile().cards()).hasSize(1);
    }

    @Test
    void multipleChoiceNeedsEveryNumberOnceAndInRange() {
        CliResult result = review("t\ny\n2\ny\n1,1\n5\n\n1 3\ny\n");

        assertThat(result.out()).contains(
                "Your answers, comma separated (1-4): Please type numbers from 1 to 4, each once, like 1,3.\n");
        assertThat(savedProfile().cards()).containsKey("basics.multi");
    }

    @Test
    void closedInputKeepsEveryAnsweredQuestion() {
        CliResult result = review("t\ny\n2\n");

        assertThat(result.exitCode()).isZero();
        assertThat(result.out())
                .endsWith("Your answer (1-3): Sure? (y/n): Input closed. Answered questions are saved.\n");
        assertThat(savedProfile().cards()).containsOnlyKeys("basics.true-false");
    }

    @Test
    void notSureRightAnswerRaisesTheEaseFactorLessThanASureOne() {
        review("t\nn\n");
        double unsure = savedProfile().cards().get("basics.true-false").easeFactor();
        cleanProfile();
        review("t\ny\n");
        double sure = savedProfile().cards().get("basics.true-false").easeFactor();

        assertThat(unsure).isLessThan(sure);
    }

    @Test
    void emptyQueuePrintsWhenTheNextReviewIs() {
        saveProfileWithAllCardsDue(TODAY.plusDays(7));

        CliResult result = review("");

        assertThat(result.exitCode()).isZero();
        assertThat(result.out()).isEqualTo("Nothing due today. Next review: 2026-11-04\n");
    }

    @Test
    void emptyQueueSavesTheQuestAsDoneOnlyOnce() {
        saveProfileWithAllCardsDue(TODAY.plusDays(7));

        review("");
        review("");

        assertThat(savedProfile().xp()).isEqualTo(50);
        assertThat(savedProfile().streak()).isEqualTo(new Streak(1, 1, TODAY));
    }

    @Test
    void finishedQueueSavesXpAndStreak() {
        review(ALL_RIGHT);

        assertThat(savedProfile().xp()).isGreaterThan(XpRules.QUEST_BONUS);
        assertThat(savedProfile().streak()).isEqualTo(new Streak(1, 1, TODAY));
    }

    @Test
    void nextReviewIsTheEarliestDueDate() {
        saveProfileWithCardsDue(Map.of(
                "basics.true-false", TODAY.plusDays(7), "basics.single", TODAY.plusDays(3),
                "basics.multi", TODAY.plusDays(7), "basics.output", TODAY.plusDays(7)));

        assertThat(review("").out()).isEqualTo("Nothing due today. Next review: 2026-10-31\n");
    }

    @Test
    void newQuestionsAreLimitedPerDayNotPerRun() {
        review(ALL_RIGHT);

        CliResult second = review("t\ny\n");
        CliResult third = review("");

        assertThat(second.out()).contains("Question 1/1 - deep");
        assertThat(third.out()).isEqualTo("Nothing due today. Next review: 2026-10-29\n");
    }

    @Test
    void damagedProfileIsRestoredFromBackupWithAWarning() throws IOException {
        review("t\ny\n2\ny\n");
        Files.writeString(profileDir.resolve("profile.json"), "{ broken");

        CliResult result = review("");

        assertThat(result.out()).startsWith("Warning: the profile file was damaged, progress is restored from the "
                + "backup.\n");
    }

    @Test
    void rightAndWrongAreColoredOnlyWhenColorsAreEnabled() {
        String colored = CliTestSupport.run("f\ny\n", new Colors(true), content, profileDir, "review").out();
        cleanProfile();
        String plain = CliTestSupport.run("f\ny\n", new Colors(false), content, profileDir, "review").out();

        assertThat(colored).contains("\u001B[31mWrong.\u001B[0m\n");
        assertThat(plain).contains("Sure? (y/n): Wrong.\n").doesNotContain("\u001B");
    }

    private CliResult review(String input) {
        return CliTestSupport.run(input, new Colors(false), content, profileDir, "review");
    }

    private PlayerProfile savedProfile() {
        return new ProfileStore(profileDir, Set.of("basics")).load().profile();
    }

    private void cleanProfile() {
        try (var files = Files.list(profileDir)) {
            files.forEach(file -> file.toFile().delete());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void saveProfileWithAllCardsDue(LocalDate due) {
        saveProfileWithCardsDue(Map.of(
                "basics.true-false", due, "basics.single", due, "basics.multi", due, "basics.output", due));
    }

    private void saveProfileWithCardsDue(Map<String, LocalDate> dueById) {
        Map<String, CardState> cards = new HashMap<>();
        dueById.forEach((id, date) -> cards.put(id, CardState.newCard(id).toBuilder()
                .dueDate(date).lastReviewDate(TODAY.minusDays(1)).intervalDays(8).repetitions(2).build()));
        PlayerProfile profile = PlayerProfile.empty(Set.of("basics")).toBuilder().cards(cards).build();
        new ProfileStore(profileDir, Set.of("basics")).save(profile);
    }
}
