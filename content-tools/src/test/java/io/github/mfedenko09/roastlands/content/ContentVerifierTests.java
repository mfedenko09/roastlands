package io.github.mfedenko09.roastlands.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mfedenko09.roastlands.core.content.CompileErrorOption;
import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.OutputOption;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import io.github.mfedenko09.roastlands.core.content.TextOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ContentVerifierTests {

    private static final Path FIXTURES = Path.of("src", "test", "resources", "fixtures");

    private static final String LINE = "pack/questions/core.types.yaml:core.types.";

    private static final String VALID_CODE = "public class Main {\n    public static void main(String[] args) {}\n}\n";

    @Test
    void correctCodeOutputQuestionPasses() {
        VerificationReport report = verifyFixture("correct-output");

        assertThat(report.failures()).isEmpty();
        assertThat(report.summary()).isEqualTo("1 passed, 0 failed");
        assertThat(report.isSuccess()).isTrue();
    }

    @Test
    void wrongAnswerKeyIsReportedWithBothOutcomes() {
        VerificationReport report = verifyFixture("wrong-key");

        assertThat(report.failures()).containsExactly(
                LINE + "wrong-key: marked option is output \"1\", actual outcome is output \"2\"");
        assertThat(report.summary()).isEqualTo("0 passed, 1 failed");
        assertThat(report.isSuccess()).isFalse();
    }

    @Test
    void otherOptionThatAlsoMatchesTheRealOutcomeIsReported() {
        assertThat(verifyFixture("matching-option").failures())
                .containsExactly(LINE + "two-options-match: option 2 also matches the actual outcome");
    }

    @Test
    void compileErrorAsTheRightAnswerPasses() {
        assertThat(verifyFixture("compile-error").summary()).isEqualTo("1 passed, 0 failed");
    }

    @Test
    void exceptionAsTheRightAnswerPasses() {
        assertThat(verifyFixture("exception").summary()).isEqualTo("1 passed, 0 failed");
    }

    @Test
    void unstableOutputIsReported() {
        assertThat(verifyFixture("unstable").failures())
                .containsExactly(LINE + "prints-time: outcome is not stable between runs");
    }

    @Test
    void endlessLoopIsReportedAsATimeout() {
        VerificationReport report = ContentCli.verify(FIXTURES.resolve("endless-loop"),
                new SnippetRunner(Duration.ofSeconds(1)));

        assertThat(report.failures()).containsExactly(LINE + "endless-loop: timeout after 1 s");
    }

    @Test
    void codeInAChoiceQuestionMustCompile() {
        List<Question> questions = List.of(
                choice("a.compiles", VALID_CODE, "p/questions/a.yaml"),
                choice("a.broken", "public class Main {", "p/questions/a.yaml"));

        VerificationReport report = verify(new ContentPack("p", questions));

        assertThat(report.failures()).containsExactly("p/questions/a.yaml:a.broken: code does not compile");
        assertThat(report.summary()).isEqualTo("1 passed, 1 failed");
    }

    @Test
    void questionWithoutCodeIsNotRun() {
        assertThat(verify(new ContentPack("p", List.of(choice("a.plain", null, "p/questions/a.yaml")))).isSuccess())
                .isTrue();
    }

    @Test
    void sameIdInTwoPacksIsReportedForTheLaterOne() {
        ContentPack first = new ContentPack("a", List.of(choice("x.same", null, "a/questions/x.yaml")));
        ContentPack second = new ContentPack("b", List.of(choice("x.same", null, "b/questions/x.yaml")));

        VerificationReport report = verify(first, second);

        assertThat(report.failures()).containsExactly(
                "b/questions/x.yaml:x.same: duplicate id, already used in a/questions/x.yaml");
        assertThat(report.summary()).isEqualTo("1 passed, 1 failed");
    }

    @Test
    void failuresKeepQuestionOrderWhenQuestionsRunInParallel() {
        List<Question> questions = IntStream.range(0, 6)
                .mapToObj(i -> choice("a.q" + i, i % 2 == 0 ? "class Broken {" : VALID_CODE, "p/questions/a.yaml"))
                .toList();

        VerificationReport report = new ContentVerifier(new SnippetRunner(), 3)
                .verify(List.of(new ContentPack("p", questions)));

        assertThat(report.failures()).containsExactly(
                "p/questions/a.yaml:a.q0: code does not compile",
                "p/questions/a.yaml:a.q2: code does not compile",
                "p/questions/a.yaml:a.q4: code does not compile");
        assertThat(report.summary()).isEqualTo("3 passed, 3 failed");
    }

    @Test
    void unexpectedErrorNamesTheQuestion() {
        Question question = choice("a.q", VALID_CODE, "p/questions/a.yaml").toBuilder().minJavaVersion(99).build();

        assertThatThrownBy(() -> verify(new ContentPack("p", List.of(question))))
                .hasMessageContaining("p/questions/a.yaml:a.q");
    }

    @Test
    void outcomeOptionsOfACodeOutputQuestionAreComparedAgainstTheRun() {
        Question question = Question.builder()
                .id("a.broken")
                .topic("a")
                .type(QuestionType.CODE_OUTPUT)
                .difficulty(1)
                .prompt("Prompt")
                .code("public class Main { public static void main(String[] args) { int x = ; } }")
                .options(List.of(new OutputOption("1"), new CompileErrorOption()))
                .correct(new SingleIndex(1))
                .explanation("Because")
                .reference("JLS §1.1")
                .minJavaVersion(21)
                .source("p/questions/a.yaml")
                .build();

        assertThat(verify(new ContentPack("p", List.of(question))).isSuccess()).isTrue();
    }

    private static VerificationReport verifyFixture(String name) {
        return ContentCli.verify(FIXTURES.resolve(name), new SnippetRunner());
    }

    private static VerificationReport verify(ContentPack... packs) {
        return new ContentVerifier(new SnippetRunner(), 2).verify(List.of(packs));
    }

    private static Question choice(String id, String code, String source) {
        return Question.builder()
                .id(id)
                .topic(id.substring(0, id.indexOf('.')))
                .type(QuestionType.SINGLE_CHOICE)
                .difficulty(1)
                .prompt("Prompt")
                .code(code)
                .options(List.of(new TextOption("yes"), new TextOption("no")))
                .correct(new SingleIndex(0))
                .explanation("Because")
                .reference("JLS §1.1")
                .minJavaVersion(21)
                .source(source)
                .build();
    }
}
