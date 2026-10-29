package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.content.Bool;
import io.github.mfedenko09.roastlands.core.content.CorrectAnswer;
import io.github.mfedenko09.roastlands.core.content.IndexSet;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import java.util.Set;

final class CorrectIndexes {

    static final int TRUE_INDEX = 0;
    static final int FALSE_INDEX = 1;

    private CorrectIndexes() {
    }

    static Set<Integer> of(CorrectAnswer correct) {
        return switch (correct) {
            case SingleIndex single -> Set.of(single.index());
            case IndexSet set -> Set.copyOf(set.indexes());
            case Bool bool -> Set.of(bool.value() ? TRUE_INDEX : FALSE_INDEX);
        };
    }
}
