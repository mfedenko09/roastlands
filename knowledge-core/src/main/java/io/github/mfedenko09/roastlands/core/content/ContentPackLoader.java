package io.github.mfedenko09.roastlands.core.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.mfedenko09.roastlands.core.topic.PackFile;
import io.github.mfedenko09.roastlands.core.topic.Topic;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ContentPackLoader {

    private static final String PACK_FILE = "pack.yaml";
    private static final String QUESTIONS_DIR = "questions";
    private static final String YAML_SUFFIX = ".yaml";

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public ContentPack load(Path packDir, TopicGraph topics) {
        PackFile pack = readPackFile(packDir);
        Set<String> knownTopics = topics.topics().stream().map(Topic::id).collect(Collectors.toSet());
        LoadedQuestions loaded = new LoadedQuestions();
        for (Path file : questionFiles(packDir.resolve(QUESTIONS_DIR))) {
            readFile(file, new ParseContext(topicOf(file), knownTopics, pack.minJavaVersion(), display(packDir, file)),
                    loaded);
        }
        if (!loaded.problems().isEmpty()) {
            throw new InvalidContentException(loaded.problems());
        }
        return new ContentPack(pack.id(), loaded.questions());
    }

    private PackFile readPackFile(Path packDir) {
        Path packFile = packDir.resolve(PACK_FILE);
        String name = display(packDir, packFile);
        try {
            PackFile pack = yaml.readValue(packFile.toFile(), PackFile.class);
            if (pack == null || pack.id() == null || pack.id().isBlank() || pack.minJavaVersion() == null) {
                throw new InvalidContentException(List.of(name + ": pack needs id and minJavaVersion"));
            }
            return pack;
        } catch (JsonProcessingException e) {
            throw new InvalidContentException(
                    List.of(name + ": " + e.getOriginalMessage() + " (line " + e.getLocation().getLineNr() + ")"));
        } catch (IOException e) {
            throw new InvalidContentException(List.of(name + ": cannot be read: " + e.getMessage()));
        }
    }

    private static List<Path> questionFiles(Path questionsDir) {
        if (!Files.isDirectory(questionsDir)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(questionsDir)) {
            return children
                    .filter(child -> Files.isRegularFile(child) && child.getFileName().toString().endsWith(YAML_SUFFIX))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list questions in " + questionsDir, e);
        }
    }

    private void readFile(Path file, ParseContext context, LoadedQuestions loaded) {
        JsonNode root;
        try {
            root = yaml.readTree(file.toFile());
        } catch (JsonProcessingException e) {
            loaded.addFileProblem(
                    context.source(), e.getOriginalMessage() + " (line " + e.getLocation().getLineNr() + ")");
            return;
        } catch (IOException e) {
            loaded.addFileProblem(context.source(), "cannot be read: " + e.getMessage());
            return;
        }
        if (!root.isArray()) {
            loaded.addFileProblem(context.source(), "file must be a list of questions");
            return;
        }
        for (int i = 0; i < root.size(); i++) {
            readQuestion(root.get(i), i + 1, context, loaded);
        }
    }

    private void readQuestion(JsonNode node, int position, ParseContext context, LoadedQuestions loaded) {
        String label = labelOf(node, position);
        if (!node.isObject()) {
            loaded.addQuestionProblem(context.source(), label, "question must be a mapping");
            return;
        }
        QuestionEntry entry;
        try {
            entry = yaml.treeToValue(node, QuestionEntry.class);
        } catch (JsonProcessingException e) {
            loaded.addQuestionProblem(context.source(), label, e.getOriginalMessage());
            return;
        }
        ParsedQuestion parsed = QuestionParser.parse(entry, context);
        if (parsed.isValid()) {
            loaded.add(parsed.question());
        } else {
            parsed.reasons().forEach(reason -> loaded.addQuestionProblem(context.source(), label, reason));
        }
    }

    private static String labelOf(JsonNode node, int position) {
        String id = node.path("id").asText("");
        return id.isBlank() ? "question #" + position : id;
    }

    private static String topicOf(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - YAML_SUFFIX.length());
    }

    private static String display(Path packDir, Path file) {
        return packDir.getFileName().resolve(packDir.relativize(file)).toString();
    }
}
