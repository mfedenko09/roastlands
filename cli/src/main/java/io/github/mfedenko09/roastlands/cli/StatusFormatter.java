package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.session.IslandProgress;
import io.github.mfedenko09.roastlands.core.session.ProgressReport;
import io.github.mfedenko09.roastlands.core.session.TopicProgress;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class StatusFormatter {

    String format(LocalDate today, ProgressReport report) {
        int titleWidth = report.islands().stream()
                .flatMap(island -> island.topics().stream())
                .mapToInt(topic -> topic.topic().title().length())
                .max()
                .orElse(0);
        List<String> lines = new ArrayList<>();
        lines.add("Roastlands status for " + today);
        for (IslandProgress island : report.islands()) {
            lines.add("");
            lines.add(island.island().title());
            island.topics().forEach(topic -> lines.add(row(topic, titleWidth)));
        }
        lines.add("");
        lines.add("Today " + report.today().done() + "/" + report.today().total());
        return String.join("\n", lines);
    }

    private static String row(TopicProgress topic, int titleWidth) {
        String row = String.format(
                "  %-" + titleWidth + "s  %-5s %3d%%",
                topic.topic().title(), topic.level(), FeedbackFormatter.percent(topic.mastery()));
        return topic.locked() ? row + "  " + lockNote(topic) : row;
    }

    private static String lockNote(TopicProgress topic) {
        return topic.weakPrerequisites().isEmpty()
                ? "locked"
                : "locked, needs: " + String.join(", ", topic.weakPrerequisites());
    }
}
