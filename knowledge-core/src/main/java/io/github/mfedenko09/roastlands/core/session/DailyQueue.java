package io.github.mfedenko09.roastlands.core.session;

import java.util.List;

public record DailyQueue(List<String> reviewCardIds, List<String> newCardIds) {

    public DailyQueue {
        reviewCardIds = List.copyOf(reviewCardIds);
        newCardIds = List.copyOf(newCardIds);
    }
}
