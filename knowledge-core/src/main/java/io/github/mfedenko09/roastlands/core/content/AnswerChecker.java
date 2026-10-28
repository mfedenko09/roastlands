package io.github.mfedenko09.roastlands.core.content;

import java.util.Set;

public final class AnswerChecker {

    public boolean isCorrect(Question question, Set<Integer> chosen) {
        return switch (question.correct()) {
            case SingleIndex single -> chosen.size() == 1 && chosen.contains(single.index());
            case IndexSet set -> chosen.equals(set.indexes());
            case Bool bool -> throw new IllegalArgumentException(question.id() + " is answered with true or false");
        };
    }

    public boolean isCorrect(Question question, boolean chosen) {
        if (question.correct() instanceof Bool bool) {
            return bool.value() == chosen;
        }
        throw new IllegalArgumentException(question.id() + " is answered with option indexes");
    }
}
