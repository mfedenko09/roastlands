package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.Bool;
import io.github.mfedenko09.roastlands.core.content.IndexSet;
import io.github.mfedenko09.roastlands.core.content.SingleIndex;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CorrectIndexesTests {

    @Test
    void singleIndexIsItself() {
        assertThat(CorrectIndexes.of(new SingleIndex(2))).containsExactly(2);
    }

    @Test
    void indexSetKeepsAllIndexes() {
        assertThat(CorrectIndexes.of(new IndexSet(Set.of(0, 3)))).containsExactlyInAnyOrder(0, 3);
    }

    @Test
    void trueIsTheFirstLabelAndFalseTheSecond() {
        assertThat(CorrectIndexes.of(new Bool(true))).containsExactly(0);
        assertThat(CorrectIndexes.of(new Bool(false))).containsExactly(1);
    }
}
