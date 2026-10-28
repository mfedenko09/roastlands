package io.github.mfedenko09.roastlands.core.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LoadedQuestions {

    private final List<Question> questions = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();
    private final Map<String, String> sourcesById = new HashMap<>();

    void addFileProblem(String file, String description) {
        problems.add(file + ": " + description);
    }

    void addQuestionProblem(String file, String label, String description) {
        problems.add(file + ":" + label + ": " + description);
    }

    void add(Question question) {
        String first = sourcesById.putIfAbsent(question.id(), question.source());
        if (first != null) {
            addQuestionProblem(question.source(), question.id(), "duplicate id, already used in " + first);
            return;
        }
        questions.add(question);
    }

    List<Question> questions() {
        return questions;
    }

    List<String> problems() {
        return problems;
    }
}
