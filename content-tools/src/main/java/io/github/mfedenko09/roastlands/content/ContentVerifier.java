package io.github.mfedenko09.roastlands.content;

import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.content.Option;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.content.QuestionType;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ContentVerifier {

    private final SnippetRunner runner;
    private final int parallelism;

    public ContentVerifier(SnippetRunner runner, int parallelism) {
        this.runner = runner;
        this.parallelism = parallelism;
    }

    public VerificationReport verify(List<ContentPack> packs) {
        List<Question> questions = packs.stream().flatMap(pack -> pack.questions().stream()).toList();
        Map<String, String> firstSourceById = new HashMap<>();
        List<String> firstSourceOfSameId = new ArrayList<>();
        for (Question question : questions) {
            firstSourceOfSameId.add(firstSourceById.putIfAbsent(question.id(), question.source()));
        }
        List<List<String>> reasons = verifyInParallel(questions);
        List<String> failures = new ArrayList<>();
        int failed = 0;
        for (int i = 0; i < questions.size(); i++) {
            List<String> own = new ArrayList<>(reasons.get(i));
            if (firstSourceOfSameId.get(i) != null) {
                own.add("duplicate id, already used in " + firstSourceOfSameId.get(i));
            }
            failed += own.isEmpty() ? 0 : 1;
            for (String reason : own) {
                failures.add(questions.get(i).source() + ":" + questions.get(i).id() + ": " + reason);
            }
        }
        return new VerificationReport(questions.size() - failed, failed, failures);
    }

    private List<List<String>> verifyInParallel(List<Question> questions) {
        ExecutorService pool = Executors.newFixedThreadPool(parallelism);
        try {
            List<CompletableFuture<List<String>>> futures = questions.stream()
                    .map(question -> CompletableFuture.supplyAsync(() -> verifyQuestion(question), pool))
                    .toList();
            return futures.stream().map(CompletableFuture::join).toList();
        } finally {
            pool.shutdown();
        }
    }

    private List<String> verifyQuestion(Question question) {
        if (question.code() == null) {
            return List.of();
        }
        try {
            return verifyCode(question);
        } catch (SnippetRunException e) {
            return List.of(e.getMessage());
        } catch (RuntimeException e) {
            throw new IllegalStateException(question.source() + ":" + question.id() + ": " + e, e);
        }
    }

    private List<String> verifyCode(Question question) {
        if (question.type() != QuestionType.CODE_OUTPUT) {
            return runner.compiles(question.code(), question.minJavaVersion())
                    ? List.of() : List.of("code does not compile");
        }
        return compareWithOptions(question, runner.run(question.code(), question.minJavaVersion()));
    }

    private static List<String> compareWithOptions(Question question, Outcome actual) {
        int markedIndex = ((SingleIndex) question.correct()).index();
        Option marked = question.options().get(markedIndex);
        if (!OutcomeMatcher.matches(marked, actual)) {
            return List.of("marked option is " + OutcomeMatcher.describe(marked)
                    + ", actual outcome is " + actual.describe());
        }
        List<String> reasons = new ArrayList<>();
        for (int i = 0; i < question.options().size(); i++) {
            if (i != markedIndex && OutcomeMatcher.matches(question.options().get(i), actual)) {
                reasons.add("option " + (i + 1) + " also matches the actual outcome");
            }
        }
        return reasons;
    }
}
