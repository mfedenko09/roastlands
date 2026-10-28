package io.github.mfedenko09.roastlands.core.content;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

final class QuestionParser {

    static final int MAX_EXPLANATION_LENGTH = 600;

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 5;
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 3;
    private static final String JLS_PREFIX = "JLS §";
    private static final String JAVADOC_PREFIX = "https://docs.oracle.com/";
    private static final String OUTPUT = "output";
    private static final String COMPILE_ERROR = "compileError";
    private static final String EXCEPTION = "exception";
    private static final Pattern NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern CLASS_NAME = Pattern.compile("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*");

    private final QuestionEntry entry;
    private final ParseContext context;
    private final List<String> reasons = new ArrayList<>();
    private List<Option> options = List.of();
    private CorrectAnswer correct;

    private QuestionParser(QuestionEntry entry, ParseContext context) {
        this.entry = entry;
        this.context = context;
    }

    static ParsedQuestion parse(QuestionEntry entry, ParseContext context) {
        return new QuestionParser(entry, context).parse();
    }

    private ParsedQuestion parse() {
        checkTopic();
        checkId();
        checkDifficulty();
        requireText("prompt", entry.prompt());
        checkExplanation();
        checkReference();
        QuestionType type = parseType();
        if (type != null) {
            parseAnswerPart(type);
        }
        return new ParsedQuestion(reasons.isEmpty() ? build(type) : null, reasons);
    }

    private void checkTopic() {
        if (isBlank(entry.topic())) {
            reasons.add("topic is required");
        } else if (!context.knownTopics().contains(entry.topic())) {
            reasons.add("unknown topic \"" + entry.topic() + "\"");
        } else if (!entry.topic().equals(context.fileTopic())) {
            reasons.add("topic must match the file name \"" + context.fileTopic() + "\"");
        }
    }

    private void checkId() {
        if (isBlank(entry.id())) {
            reasons.add("id is required");
        } else if (!isBlank(entry.topic()) && !hasTopicPrefixAndKebabName(entry.id(), entry.topic())) {
            reasons.add("id must be \"" + entry.topic() + ".<kebab-case-name>\"");
        }
    }

    private static boolean hasTopicPrefixAndKebabName(String id, String topic) {
        String prefix = topic + ".";
        return id.startsWith(prefix) && NAME.matcher(id.substring(prefix.length())).matches();
    }

    private void checkDifficulty() {
        Integer difficulty = entry.difficulty();
        if (difficulty == null) {
            reasons.add("difficulty is required");
        } else if (difficulty < MIN_DIFFICULTY || difficulty > MAX_DIFFICULTY) {
            reasons.add("difficulty must be 1, 2 or 3");
        }
    }

    private void checkExplanation() {
        if (requireText("explanation", entry.explanation()) && entry.explanation().length() > MAX_EXPLANATION_LENGTH) {
            reasons.add("explanation is longer than " + MAX_EXPLANATION_LENGTH + " characters");
        }
    }

    private void checkReference() {
        if (requireText("reference", entry.reference())
                && !entry.reference().startsWith(JLS_PREFIX) && !entry.reference().startsWith(JAVADOC_PREFIX)) {
            reasons.add("reference must be a JLS section (\"JLS §...\") or a docs.oracle.com Javadoc URL");
        }
    }

    private QuestionType parseType() {
        if (!requireText("type", entry.type())) {
            return null;
        }
        QuestionType type = Arrays.stream(QuestionType.values())
                .filter(candidate -> candidate.name().equals(entry.type()))
                .findFirst()
                .orElse(null);
        if (type == null) {
            reasons.add("unknown type \"" + entry.type() + "\"");
        }
        return type;
    }

    private void parseAnswerPart(QuestionType type) {
        switch (type) {
            case CODE_OUTPUT -> {
                requireText("code", entry.code());
                options = parseOptions(this::outcomeOption);
                correct = parseSingleIndex();
            }
            case SINGLE_CHOICE -> {
                options = parseOptions(this::textOption);
                correct = parseSingleIndex();
            }
            case MULTI_CHOICE -> {
                options = parseOptions(this::textOption);
                correct = parseIndexSet();
            }
            case TRUE_FALSE -> {
                if (!isAbsent(entry.options())) {
                    reasons.add("TRUE_FALSE question must not have options");
                }
                correct = parseBool();
            }
        }
    }

    private List<Option> parseOptions(BiFunction<JsonNode, Integer, Option> reader) {
        JsonNode node = entry.options();
        if (isAbsent(node) || !node.isArray() || node.size() < MIN_OPTIONS || node.size() > MAX_OPTIONS) {
            reasons.add("options must be a list of " + MIN_OPTIONS + " to " + MAX_OPTIONS + " entries");
            return List.of();
        }
        List<Option> parsed = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            parsed.add(reader.apply(node.get(i), i + 1));
        }
        reportEqualOptions(parsed);
        return parsed;
    }

    private void reportEqualOptions(List<Option> parsed) {
        for (int first = 0; first < parsed.size(); first++) {
            for (int second = first + 1; second < parsed.size(); second++) {
                if (parsed.get(first) != null && parsed.get(first).equals(parsed.get(second))) {
                    reasons.add("options " + (first + 1) + " and " + (second + 1) + " are the same");
                }
            }
        }
    }

    private Option textOption(JsonNode node, int number) {
        if (!node.isTextual() || node.asText().isBlank()) {
            reasons.add("option " + number + " must be a non-empty string");
            return null;
        }
        return new TextOption(node.asText());
    }

    private Option outcomeOption(JsonNode node, int number) {
        Set<String> keys = new HashSet<>();
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(keys::add);
        }
        if (keys.equals(Set.of(OUTPUT))) {
            String output = text(node, OUTPUT, number);
            return output == null ? null : new OutputOption(output);
        }
        if (keys.equals(Set.of(COMPILE_ERROR))) {
            return compileErrorOption(node, number);
        }
        if (keys.contains(EXCEPTION) && Set.of(EXCEPTION, OUTPUT).containsAll(keys)) {
            return exceptionOption(node, number);
        }
        reasons.add("option " + number + " must be output, compileError or exception (with optional output)");
        return null;
    }

    private Option compileErrorOption(JsonNode node, int number) {
        if (!node.get(COMPILE_ERROR).isBoolean() || !node.get(COMPILE_ERROR).asBoolean()) {
            reasons.add("option " + number + ": compileError must be true");
            return null;
        }
        return new CompileErrorOption();
    }

    private Option exceptionOption(JsonNode node, int number) {
        String exception = text(node, EXCEPTION, number);
        if (exception == null) {
            return null;
        }
        if (!CLASS_NAME.matcher(exception).matches()) {
            reasons.add("option " + number + ": exception must be a fully qualified class name");
            return null;
        }
        if (!node.has(OUTPUT)) {
            return new ExceptionOption(exception, null);
        }
        String output = text(node, OUTPUT, number);
        return output == null ? null : new ExceptionOption(exception, output);
    }

    private String text(JsonNode node, String field, int number) {
        if (!node.get(field).isTextual()) {
            reasons.add("option " + number + ": " + field + " must be a string");
            return null;
        }
        return node.get(field).asText();
    }

    private CorrectAnswer parseSingleIndex() {
        JsonNode node = entry.correct();
        if (options.isEmpty()) {
            return null;
        }
        if (isAbsent(node)) {
            reasons.add("correct is required");
            return null;
        }
        if (!node.isInt() || !isValidIndex(node.asInt())) {
            reasons.add("correct must be an option index from 0 to " + (options.size() - 1));
            return null;
        }
        return new SingleIndex(node.asInt());
    }

    private CorrectAnswer parseIndexSet() {
        JsonNode node = entry.correct();
        if (options.isEmpty()) {
            return null;
        }
        if (isAbsent(node)) {
            reasons.add("correct is required");
            return null;
        }
        Set<Integer> indexes = new HashSet<>();
        boolean valid = node.isArray() && !node.isEmpty();
        if (valid) {
            valid = StreamSupport.stream(node.spliterator(), false)
                    .allMatch(item -> item.isInt() && isValidIndex(item.asInt()) && indexes.add(item.asInt()));
        }
        if (!valid) {
            reasons.add("correct must be a list of distinct option indexes from 0 to " + (options.size() - 1));
            return null;
        }
        return new IndexSet(indexes);
    }

    private CorrectAnswer parseBool() {
        JsonNode node = entry.correct();
        if (isAbsent(node)) {
            reasons.add("correct is required");
            return null;
        }
        if (!node.isBoolean()) {
            reasons.add("correct must be true or false");
            return null;
        }
        return new Bool(node.asBoolean());
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < options.size();
    }

    private boolean requireText(String field, String value) {
        if (isBlank(value)) {
            reasons.add(field + " is required");
            return false;
        }
        return true;
    }

    private Question build(QuestionType type) {
        return Question.builder()
                .id(entry.id())
                .topic(entry.topic())
                .type(type)
                .difficulty(entry.difficulty())
                .prompt(entry.prompt())
                .code(entry.code())
                .options(options)
                .correct(correct)
                .explanation(entry.explanation())
                .reference(entry.reference())
                .minJavaVersion(entry.minJavaVersion() != null ? entry.minJavaVersion() : context.packMinJavaVersion())
                .source(context.source())
                .build();
    }

    private static boolean isAbsent(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
