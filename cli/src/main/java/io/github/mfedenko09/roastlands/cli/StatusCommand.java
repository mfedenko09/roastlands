package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.profile.ProfileStore;
import io.github.mfedenko09.roastlands.core.session.ProgressReport;
import io.github.mfedenko09.roastlands.core.session.ProgressReporter;
import io.github.mfedenko09.roastlands.core.session.SessionLimits;
import java.time.LocalDate;

final class StatusCommand {

    private final GameContent content;
    private final ProfileStore store;
    private final Terminal terminal;
    private final LocalDate today;
    private final StatusFormatter formatter = new StatusFormatter();

    StatusCommand(GameContent content, ProfileStore store, Terminal terminal, LocalDate today) {
        this.content = content;
        this.store = store;
        this.terminal = terminal;
        this.today = today;
    }

    int run() {
        ProgressReport report = new ProgressReporter(content.topics(), content.pack())
                .report(store.load().profile(), today, SessionLimits.DEFAULT);
        terminal.println(formatter.format(today, report));
        return 0;
    }
}
