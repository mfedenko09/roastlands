package io.github.mfedenko09.roastlands.core.session;

import java.util.List;

public record ProgressReport(List<IslandProgress> islands, SessionProgress today) {

    public ProgressReport {
        islands = List.copyOf(islands);
    }
}
