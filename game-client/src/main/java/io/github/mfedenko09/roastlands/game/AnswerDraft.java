package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.content.QuestionType;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

final class AnswerDraft {

    private final boolean multiChoice;
    private final int optionCount;
    private final Set<Integer> selected = new TreeSet<>();

    AnswerDraft(QuestionType type, int optionCount) {
        this.multiChoice = type == QuestionType.MULTI_CHOICE;
        this.optionCount = optionCount;
    }

    boolean isMultiChoice() {
        return multiChoice;
    }

    boolean isEmpty() {
        return selected.isEmpty();
    }

    Set<Integer> selected() {
        return Collections.unmodifiableSet(selected);
    }

    void pick(int index) {
        if (index < 0 || index >= optionCount) {
            return;
        }
        if (!multiChoice) {
            selected.clear();
            selected.add(index);
        } else if (!selected.remove(index)) {
            selected.add(index);
        }
    }
}
