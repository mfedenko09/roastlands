package io.github.mfedenko09.roastlands.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NextReviewTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final List<Question> QUESTIONS = List.of(
            Question.builder().id("a").options(List.of()).build(),
            Question.builder().id("b").options(List.of()).build());

    private final NextReview nextReview = new NextReview();

    @Test
    void earliestFutureDueDateOfKnownQuestionsWins() {
        List<CardState> cards = List.of(card("a", TODAY.plusDays(5)), card("b", TODAY.plusDays(2)));

        assertThat(nextReview.after(cards, QUESTIONS, TODAY)).contains(LocalDate.of(2026, 10, 30));
    }

    @Test
    void cardsDueTodayOrBeforeAndCardsOfRemovedQuestionsAreIgnored() {
        List<CardState> cards = List.of(
                card("a", TODAY), card("b", TODAY.minusDays(1)), card("removed", TODAY.plusDays(1)));

        assertThat(nextReview.after(cards, QUESTIONS, TODAY)).isEmpty();
    }

    private static CardState card(String id, LocalDate due) {
        return CardState.newCard(id).toBuilder().dueDate(due).build();
    }
}
