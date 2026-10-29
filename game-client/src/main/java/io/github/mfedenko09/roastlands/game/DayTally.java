package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.session.AnswerResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class DayTally {

    private static final int PERCENT = 100;

    private final List<AnswerResult> results = new ArrayList<>();

    void record(AnswerResult result) {
        results.add(result);
    }

    void replaceLast(AnswerResult result) {
        results.set(results.size() - 1, result);
    }

    DaySummary summary() {
        int correct = (int) results.stream().filter(AnswerResult::correct).count();
        Set<String> unlocked = new LinkedHashSet<>();
        results.forEach(result -> unlocked.addAll(result.topicsUnlocked()));
        return new DaySummary(results.size(), correct, percent(correct, results.size()), List.copyOf(unlocked));
    }

    private static int percent(int correct, int answered) {
        return answered == 0 ? 0 : (int) Math.round((double) PERCENT * correct / answered);
    }
}
