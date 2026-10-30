package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.mastery.MasteryLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class IslandAccordionTests {

    private static final IslandView LOCKED = island(true);
    private static final IslandView OPEN = island(false);

    @Test
    void startsOnTheFirstIslandWithAnOpenTopic() {
        IslandAccordion accordion = IslandAccordion.openingFirstOpenIsland(List.of(LOCKED, OPEN, OPEN));

        assertThat(accordion.isExpanded(1)).isTrue();
        assertThat(accordion.isExpanded(2)).isFalse();
    }

    @Test
    void startsCollapsedWhenEveryIslandIsLocked() {
        IslandAccordion accordion = IslandAccordion.openingFirstOpenIsland(List.of(LOCKED, LOCKED));

        assertThat(accordion.isExpanded(0)).isFalse();
        assertThat(accordion.isExpanded(1)).isFalse();
    }

    @Test
    void togglingTheOpenSectionCollapsesIt() {
        IslandAccordion accordion = IslandAccordion.openingFirstOpenIsland(List.of(OPEN, OPEN));

        accordion.toggle(0);

        assertThat(accordion.isExpanded(0)).isFalse();
    }

    @Test
    void openingASectionClosesThePreviousOne() {
        IslandAccordion accordion = IslandAccordion.openingFirstOpenIsland(List.of(OPEN, OPEN));

        accordion.toggle(1);

        assertThat(accordion.isExpanded(0)).isFalse();
        assertThat(accordion.isExpanded(1)).isTrue();
    }

    @Test
    void movingStopsAtBothEnds() {
        IslandAccordion accordion = IslandAccordion.openingFirstOpenIsland(List.of(OPEN, OPEN));

        accordion.move(-1);
        assertThat(accordion.isExpanded(0)).isTrue();

        accordion.move(1);
        accordion.move(1);
        assertThat(accordion.isExpanded(1)).isTrue();
    }

    @Test
    void movingAfterCollapsingContinuesFromTheCollapsedSection() {
        IslandAccordion accordion = IslandAccordion.openingFirstOpenIsland(List.of(OPEN, OPEN, OPEN, OPEN));
        accordion.toggle(2);
        accordion.toggle(2);

        accordion.move(1);

        assertThat(accordion.isExpanded(3)).isTrue();
    }

    @Test
    void movingWithEverySectionLockedOpensTheNextOne() {
        IslandAccordion accordion = IslandAccordion.openingFirstOpenIsland(List.of(LOCKED, LOCKED));

        accordion.move(1);

        assertThat(accordion.isExpanded(1)).isTrue();
    }

    private static IslandView island(boolean locked) {
        TopicView topic = TopicView.builder().title("t").level(MasteryLevel.NEW).progress("0%").locked(locked)
                .needs("").build();
        return new IslandView("i", "s", List.of(topic));
    }
}
