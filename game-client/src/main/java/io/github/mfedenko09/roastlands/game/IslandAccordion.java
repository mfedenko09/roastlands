package io.github.mfedenko09.roastlands.game;

import java.util.List;

final class IslandAccordion {

    private final int islandCount;
    private int cursor;
    private boolean expanded;

    private IslandAccordion(int islandCount, int cursor, boolean expanded) {
        this.islandCount = islandCount;
        this.cursor = cursor;
        this.expanded = expanded;
    }

    static IslandAccordion openingFirstOpenIsland(List<IslandView> islands) {
        for (int index = 0; index < islands.size(); index++) {
            if (islands.get(index).hasOpenTopics()) {
                return new IslandAccordion(islands.size(), index, true);
            }
        }
        return new IslandAccordion(islands.size(), 0, false);
    }

    boolean isExpanded(int index) {
        return expanded && cursor == index;
    }

    void toggle(int index) {
        expanded = !isExpanded(index);
        cursor = index;
    }

    void move(int delta) {
        cursor = Math.max(0, Math.min(islandCount - 1, cursor + delta));
        expanded = true;
    }
}
