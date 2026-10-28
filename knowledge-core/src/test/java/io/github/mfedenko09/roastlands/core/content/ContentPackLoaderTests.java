package io.github.mfedenko09.roastlands.core.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.mfedenko09.roastlands.core.topic.Island;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ContentPackLoaderTests {

    private static final String PACK = """
            id: sample
            title: Sample
            version: 1.0.0
            minJavaVersion: 21
            """;

    private static final String CODE_OUTPUT = """
            - id: core.types.sample
              topic: core.types
              type: CODE_OUTPUT
              difficulty: 1
              prompt: What does this print?
              code: |
                public class Main {}
              options:
                - output: "a"
                - output: "b"
              correct: 0
              explanation: Because.
              reference: JLS §1.1
            """;

    private static final String SINGLE_CHOICE = """
            - id: core.types.sample
              topic: core.types
              type: SINGLE_CHOICE
              difficulty: 2
              prompt: Which one?
              options: [first, second, third]
              correct: 2
              explanation: Because.
              reference: https://docs.oracle.com/en/java/javase/21/docs/api/
            """;

    private static final String MULTI_CHOICE = """
            - id: core.types.sample
              topic: core.types
              type: MULTI_CHOICE
              difficulty: 3
              prompt: Which ones?
              options: [first, second, third]
              correct: [0, 2]
              explanation: Because.
              reference: JLS §1.1
            """;

    private static final String TRUE_FALSE = """
            - id: core.types.sample
              topic: core.types
              type: TRUE_FALSE
              difficulty: 1
              prompt: Is it so?
              correct: false
              explanation: Because.
              reference: JLS §1.1
            """;

    private static final String FILE = "sample/questions/core.types.yaml";

    private final ContentPackLoader loader = new ContentPackLoader();

    private final TopicGraph topics = TopicGraph.of(
            List.of(new Island("shore", "Shore")),
            List.of(topic("core.types"), topic("core.strings")));

    @TempDir
    Path root;

    @Test
    void loadsEveryQuestionTypeWithTypedOptionsAndAnswers() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT);
        writeQuestions("core.strings", as(SINGLE_CHOICE, "core.strings", "single") + "\n"
                + as(MULTI_CHOICE, "core.strings", "multi") + "\n" + as(TRUE_FALSE, "core.strings", "bool"));

        ContentPack pack = loader.load(packDir(), topics);

        assertThat(pack.id()).isEqualTo("sample");
        assertThat(pack.questions()).extracting(Question::id).containsExactly(
                "core.strings.single", "core.strings.multi", "core.strings.bool", "core.types.sample");
        Question single = pack.questions().get(0);
        assertThat(single.options())
                .containsExactly(new TextOption("first"), new TextOption("second"), new TextOption("third"));
        assertThat(single.correct()).isEqualTo(new SingleIndex(2));
        assertThat(pack.questions().get(1).correct()).isEqualTo(new IndexSet(Set.of(0, 2)));
        assertThat(pack.questions().get(2).options()).isEmpty();
        assertThat(pack.questions().get(2).correct()).isEqualTo(new Bool(false));
        Question codeOutput = pack.questions().get(3);
        assertThat(codeOutput.options()).containsExactly(new OutputOption("a"), new OutputOption("b"));
        assertThat(codeOutput.correct()).isEqualTo(new SingleIndex(0));
        assertThat(codeOutput.code()).isEqualTo("public class Main {}\n");
        assertThat(codeOutput.source()).isEqualTo(FILE);
    }

    @Test
    void readsAllThreeKindsOfCodeOutcomes() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT.replace("""
                    - output: "a"
                    - output: "b"
                """, """
                    - compileError: true
                    - exception: java.lang.IllegalStateException
                    - exception: java.lang.IllegalStateException
                      output: "start"
                """).replace("correct: 0", "correct: 1"));

        Question question = loader.load(packDir(), topics).questions().get(0);

        assertThat(question.options()).containsExactly(
                new CompileErrorOption(),
                new ExceptionOption("java.lang.IllegalStateException", null),
                new ExceptionOption("java.lang.IllegalStateException", "start"));
        assertThat(((ExceptionOption) question.options().get(1)).checksOutput()).isFalse();
        assertThat(((ExceptionOption) question.options().get(2)).checksOutput()).isTrue();
    }

    @Test
    void emptyOutputIsAValidOutcome() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT.replace("output: \"a\"", "output: \"\""));

        assertThat(loader.load(packDir(), topics).questions().get(0).options().get(0))
                .isEqualTo(new OutputOption(""));
    }

    @Test
    void minJavaVersionFallsBackToThePackAndCanBeOverridden() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT + "\n"
                + as(CODE_OUTPUT, "core.types", "other")
                        .replace("  correct: 0\n", "  correct: 0\n  minJavaVersion: 17\n"));

        List<Question> questions = loader.load(packDir(), topics).questions();

        assertThat(questions).extracting(Question::minJavaVersion).containsExactly(21, 17);
    }

    @Test
    void packWithoutQuestionsDirectoryHasNoQuestions() throws IOException {
        writePackFile(PACK);

        assertThat(loader.load(packDir(), topics).questions()).isEmpty();
    }

    @Test
    void filesThatAreNotYamlAndSubdirectoriesAreIgnored() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT);
        Files.writeString(questionsDir().resolve("notes.md"), "not yaml: [");
        Files.createDirectories(questionsDir().resolve("drafts.yaml"));

        assertThat(loader.load(packDir(), topics).questions()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void difficultyBoundsInsideTheRangeAreAccepted(int difficulty) throws IOException {
        writeQuestions("core.types", CODE_OUTPUT.replace("difficulty: 1", "difficulty: " + difficulty));

        assertThat(loader.load(packDir(), topics).questions().get(0).difficulty()).isEqualTo(difficulty);
    }

    @Test
    void fiveOptionsAreAccepted() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT.replace("    - output: \"b\"\n",
                "    - output: \"b\"\n    - output: \"c\"\n    - output: \"d\"\n    - output: \"e\"\n"));

        assertThat(loader.load(packDir(), topics).questions().get(0).options()).hasSize(5);
    }

    @Test
    void explanationOfExactlyTheLimitIsAccepted() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT.replace("Because.", "x".repeat(600)));

        assertThat(loader.load(packDir(), topics).questions()).hasSize(1);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("schemaErrors")
    void everySchemaErrorGivesTheExpectedLine(String yaml, String fileTopic, String expectedLines) throws IOException {
        writeQuestions(fileTopic, yaml);

        assertProblems(expectedLines.split("\n"));
    }

    static Stream<Arguments> schemaErrors() {
        String codeLine = "sample/questions/core.types.yaml:core.types.sample: ";
        return Stream.of(
                error(CODE_OUTPUT.replace("difficulty: 1", "difficulty: 4"), codeLine + "difficulty must be 1, 2 or 3"),
                error(CODE_OUTPUT.replace("difficulty: 1", "difficulty: 0"), codeLine + "difficulty must be 1, 2 or 3"),
                error(CODE_OUTPUT.replace("  difficulty: 1\n", ""), codeLine + "difficulty is required"),
                error(CODE_OUTPUT.replace("  prompt: What does this print?\n", ""), codeLine + "prompt is required"),
                error(CODE_OUTPUT.replace("  explanation: Because.\n", ""), codeLine + "explanation is required"),
                error(CODE_OUTPUT.replace("Because.", "x".repeat(601)),
                        codeLine + "explanation is longer than 600 characters"),
                error(CODE_OUTPUT.replace("JLS §1.1", "Effective Java, item 7"), codeLine
                        + "reference must be a JLS section (\"JLS §...\") or a docs.oracle.com Javadoc URL"),
                error(CODE_OUTPUT.replace("  reference: JLS §1.1\n", ""), codeLine + "reference is required"),
                error(CODE_OUTPUT.replace("CODE_OUTPUT", "ESSAY"), codeLine + "unknown type \"ESSAY\""),
                error(CODE_OUTPUT.replace("  type: CODE_OUTPUT\n", ""), codeLine + "type is required"),
                error(CODE_OUTPUT.replace("  code: |\n    public class Main {}\n", ""), codeLine + "code is required"),
                error(CODE_OUTPUT.replace("  correct: 0\n", ""), codeLine + "correct is required"),
                error(CODE_OUTPUT.replace("correct: 0", "correct: 2"),
                        codeLine + "correct must be an option index from 0 to 1"),
                error(CODE_OUTPUT.replace("correct: 0", "correct: -1"),
                        codeLine + "correct must be an option index from 0 to 1"),
                error(CODE_OUTPUT.replace("correct: 0", "correct: first"),
                        codeLine + "correct must be an option index from 0 to 1"),
                error(CODE_OUTPUT.replace("correct: 0", "correct: [0]"),
                        codeLine + "correct must be an option index from 0 to 1"),
                error(CODE_OUTPUT.replace("    - output: \"b\"\n", ""),
                        codeLine + "options must be a list of 2 to 5 entries"),
                error(CODE_OUTPUT.replace("    - output: \"b\"\n", "    - output: \"b\"\n".repeat(5)),
                        codeLine + "options must be a list of 2 to 5 entries"),
                error(CODE_OUTPUT.replace("  options:\n    - output: \"a\"\n    - output: \"b\"\n", ""),
                        codeLine + "options must be a list of 2 to 5 entries"),
                error(CODE_OUTPUT.replace("output: \"b\"", "output: \"a\""),
                        codeLine + "options 1 and 2 are the same"),
                error(CODE_OUTPUT.replace("output: \"b\"", "answer: \"b\""), codeLine
                        + "option 2 must be output, compileError or exception (with optional output)"),
                error(CODE_OUTPUT.replace("output: \"b\"", "just text"), codeLine
                        + "option 2 must be output, compileError or exception (with optional output)"),
                error(CODE_OUTPUT.replace("output: \"b\"", "output: 5"),
                        codeLine + "option 2: output must be a string"),
                error(CODE_OUTPUT.replace("output: \"b\"", "compileError: false"),
                        codeLine + "option 2: compileError must be true"),
                error(CODE_OUTPUT.replace("output: \"b\"", "compileError: yes please"),
                        codeLine + "option 2: compileError must be true"),
                error(CODE_OUTPUT.replace("output: \"b\"", "exception: 7"),
                        codeLine + "option 2: exception must be a string"),
                error(CODE_OUTPUT.replace("output: \"b\"", "exception: not a class"),
                        codeLine + "option 2: exception must be a fully qualified class name"),
                error(CODE_OUTPUT.replace("output: \"b\"", "exception: java.lang.Error\n      output: 5"),
                        codeLine + "option 2: output must be a string"),
                error(CODE_OUTPUT.replace("output: \"b\"", "exception: java.lang.Error\n      extra: 1"), codeLine
                        + "option 2 must be output, compileError or exception (with optional output)"),
                error(CODE_OUTPUT.replace("core.types.sample", "other.sample"),
                        "sample/questions/core.types.yaml:other.sample: id must be \"core.types.<kebab-case-name>\""),
                error(CODE_OUTPUT.replace("core.types.sample", "core.types.Bad_Name"),
                        "sample/questions/core.types.yaml:core.types.Bad_Name: "
                                + "id must be \"core.types.<kebab-case-name>\""),
                error(CODE_OUTPUT.replace("- id: core.types.sample\n  topic: core.types\n", "- topic: core.types\n"),
                        "sample/questions/core.types.yaml:question #1: id is required"),
                error(CODE_OUTPUT.replace("  topic: core.types\n", ""),
                        codeLine + "topic is required"),
                error(as(CODE_OUTPUT, "core.strings", "sample"),
                        "sample/questions/core.types.yaml:core.strings.sample: "
                                + "topic must match the file name \"core.types\""),
                error(SINGLE_CHOICE.replace("options: [first, second, third]", "options: [first, '', third]"),
                        codeLine + "option 2 must be a non-empty string"),
                error(SINGLE_CHOICE.replace("options: [first, second, third]", "options: [first, {a: b}, third]"),
                        codeLine + "option 2 must be a non-empty string"),
                error(SINGLE_CHOICE.replace("[first, second, third]", "[first, second, second]"),
                        codeLine + "options 2 and 3 are the same"),
                error(SINGLE_CHOICE.replace("correct: 2", "correct: 3"),
                        codeLine + "correct must be an option index from 0 to 2"),
                error(MULTI_CHOICE.replace("correct: [0, 2]", "correct: 1"),
                        codeLine + "correct must be a list of distinct option indexes from 0 to 2"),
                error(MULTI_CHOICE.replace("correct: [0, 2]", "correct: [0, 0]"),
                        codeLine + "correct must be a list of distinct option indexes from 0 to 2"),
                error(MULTI_CHOICE.replace("correct: [0, 2]", "correct: [0, 3]"),
                        codeLine + "correct must be a list of distinct option indexes from 0 to 2"),
                error(MULTI_CHOICE.replace("correct: [0, 2]", "correct: [-1]"),
                        codeLine + "correct must be a list of distinct option indexes from 0 to 2"),
                error(MULTI_CHOICE.replace("correct: [0, 2]", "correct: [a]"),
                        codeLine + "correct must be a list of distinct option indexes from 0 to 2"),
                error(MULTI_CHOICE.replace("correct: [0, 2]", "correct: []"),
                        codeLine + "correct must be a list of distinct option indexes from 0 to 2"),
                error(MULTI_CHOICE.replace("  correct: [0, 2]\n", ""), codeLine + "correct is required"),
                error(TRUE_FALSE.replace("correct: false", "correct: maybe"),
                        codeLine + "correct must be true or false"),
                error(TRUE_FALSE.replace("  correct: false\n", ""), codeLine + "correct is required"),
                error(TRUE_FALSE.replace("  correct", "  options: [yes, no]\n  correct"),
                        codeLine + "TRUE_FALSE question must not have options"));
    }

    @Test
    void unknownTopicNamesTheFileTheQuestionAndTheTopic() throws IOException {
        writeQuestions("core.typo", CODE_OUTPUT.replace("core.types", "core.typo"));

        assertProblems("sample/questions/core.typo.yaml:core.typo.sample: unknown topic \"core.typo\"");
    }

    @Test
    void duplicateIdInOneFileIsReported() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT + "\n" + CODE_OUTPUT);

        assertProblems("sample/questions/core.types.yaml:core.types.sample: duplicate id, already used in "
                + "sample/questions/core.types.yaml");
    }

    @Test
    void allProblemsFromAllFilesAreCollectedInFileOrder() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT.replace("difficulty: 1", "difficulty: 9") + "\n"
                + CODE_OUTPUT.replace("sample", "second").replace("correct: 0", "correct: 5"));
        writeQuestions("core.strings", TRUE_FALSE.replace("core.types", "core.strings").replace("false", "maybe"));

        assertProblems(
                "sample/questions/core.strings.yaml:core.strings.sample: correct must be true or false",
                "sample/questions/core.types.yaml:core.types.sample: difficulty must be 1, 2 or 3",
                "sample/questions/core.types.yaml:core.types.second: correct must be an option index from 0 to 1");
    }

    @Test
    void unknownFieldNamesTheFileAndTheQuestion() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT.replace("  correct: 0", "  correct: 0\n  bogus: 1"));

        assertThat(problems()).singleElement().satisfies(line -> assertThat(line)
                .startsWith("sample/questions/core.types.yaml:core.types.sample: ").contains("bogus"));
    }

    @Test
    void wrongValueTypeNamesTheFileAndTheQuestion() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT.replace("difficulty: 1", "difficulty: hard"));

        assertThat(problems()).singleElement().satisfies(line -> assertThat(line)
                .startsWith("sample/questions/core.types.yaml:core.types.sample: ").contains("hard"));
    }

    @Test
    void fileThatIsNotAListIsRejected() throws IOException {
        writeQuestions("core.types", "id: core.types.sample\n");

        assertProblems("sample/questions/core.types.yaml: file must be a list of questions");
    }

    @Test
    void emptyFileIsRejected() throws IOException {
        writeQuestions("core.types", "");

        assertProblems("sample/questions/core.types.yaml: file must be a list of questions");
    }

    @Test
    void brokenYamlSyntaxReportsTheLine() throws IOException {
        writeQuestions("core.types", "- id: [unclosed\n");

        assertThat(problems()).singleElement().satisfies(line -> assertThat(line)
                .startsWith("sample/questions/core.types.yaml: ").contains("(line 1)"));
    }

    @Test
    void listItemThatIsNotAMappingIsReportedByPosition() throws IOException {
        writeQuestions("core.types", CODE_OUTPUT + "- just text\n");

        assertProblems("sample/questions/core.types.yaml:question #2: question must be a mapping");
    }

    @Test
    void missingPackFileIsReported() throws IOException {
        Files.createDirectories(packDir());

        assertThat(problems()).singleElement().satisfies(line -> assertThat(line)
                .startsWith("sample/pack.yaml: cannot be read: "));
    }

    @Test
    void packFileWithoutIdIsReported() throws IOException {
        writePackFile("title: Sample\nminJavaVersion: 21\n");

        assertProblems("sample/pack.yaml: pack needs id and minJavaVersion");
    }

    @Test
    void packFileWithoutMinJavaVersionIsReported() throws IOException {
        writePackFile("id: sample\ntitle: Sample\nversion: 1.0.0\n");

        assertProblems("sample/pack.yaml: pack needs id and minJavaVersion");
    }

    @Test
    void nullPackFileIsReported() throws IOException {
        writePackFile("~\n");

        assertProblems("sample/pack.yaml: pack needs id and minJavaVersion");
    }

    @Test
    void emptyPackFileIsReported() throws IOException {
        writePackFile("");

        assertThat(problems()).singleElement().satisfies(line -> assertThat(line).startsWith("sample/pack.yaml: "));
    }

    @Test
    void brokenPackFileReportsTheLine() throws IOException {
        writePackFile("id: [unclosed\n");

        assertThat(problems()).singleElement().satisfies(line -> assertThat(line)
                .startsWith("sample/pack.yaml: ").contains("(line 1)"));
    }

    private static String as(String template, String topic, String name) {
        return template.replace("id: core.types.sample", "id: " + topic + "." + name)
                .replace("topic: core.types", "topic: " + topic);
    }

    private static Arguments error(String yaml, String... lines) {
        String fileTopic = lines[0].substring(lines[0].indexOf("questions/") + "questions/".length(),
                lines[0].indexOf(".yaml:"));
        return Arguments.of(yaml, fileTopic, String.join("\n", lines));
    }

    private void assertProblems(String... expectedLines) {
        assertThat(problems()).containsExactly(expectedLines);
    }

    private List<String> problems() {
        InvalidContentException thrown = catchThrowableOfType(InvalidContentException.class,
                () -> loader.load(packDir(), topics));
        assertThat(thrown).as("InvalidContentException").isNotNull();
        return thrown.problems();
    }

    private Path packDir() {
        return root.resolve("sample");
    }

    private Path questionsDir() {
        return packDir().resolve("questions");
    }

    private void writePackFile(String content) throws IOException {
        Files.createDirectories(packDir());
        Files.writeString(packDir().resolve("pack.yaml"), content);
    }

    private void writeQuestions(String topic, String yaml) throws IOException {
        if (!Files.exists(packDir().resolve("pack.yaml"))) {
            writePackFile(PACK);
        }
        Files.createDirectories(questionsDir());
        Files.writeString(questionsDir().resolve(topic + ".yaml"), yaml);
    }

    private static Topic topic(String id) {
        return Topic.builder()
                .id(id)
                .title(id)
                .islandId("shore")
                .prerequisites(List.of())
                .minJavaVersion(21)
                .keyPoints(List.of())
                .build();
    }
}
