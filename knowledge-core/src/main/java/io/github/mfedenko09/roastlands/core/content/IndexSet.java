package io.github.mfedenko09.roastlands.core.content;

import java.util.Set;

public record IndexSet(Set<Integer> indexes) implements CorrectAnswer {

    public IndexSet {
        indexes = Set.copyOf(indexes);
    }
}
