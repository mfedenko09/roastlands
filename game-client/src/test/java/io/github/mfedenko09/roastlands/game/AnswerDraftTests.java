package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.QuestionType;
import org.junit.jupiter.api.Test;

class AnswerDraftTests {

    @Test
    void singleChoiceKeepsOnlyTheLastPick() {
        AnswerDraft draft = new AnswerDraft(QuestionType.SINGLE_CHOICE, 4);

        draft.pick(1);
        draft.pick(3);

        assertThat(draft.selected()).containsExactly(3);
        assertThat(draft.isMultiChoice()).isFalse();
    }

    @Test
    void multiChoiceTogglesAndKeepsIndexesSorted() {
        AnswerDraft draft = new AnswerDraft(QuestionType.MULTI_CHOICE, 4);

        draft.pick(2);
        draft.pick(0);
        draft.pick(2);
        draft.pick(3);

        assertThat(draft.selected()).containsExactly(0, 3);
        assertThat(draft.isMultiChoice()).isTrue();
    }

    @Test
    void indexesOutsideTheOptionsAreIgnoredOnBothEnds() {
        AnswerDraft draft = new AnswerDraft(QuestionType.SINGLE_CHOICE, 3);

        draft.pick(-1);
        draft.pick(3);

        assertThat(draft.isEmpty()).isTrue();
    }

    @Test
    void lastValidIndexIsAccepted() {
        AnswerDraft draft = new AnswerDraft(QuestionType.SINGLE_CHOICE, 3);

        draft.pick(2);

        assertThat(draft.selected()).containsExactly(2);
    }
}
