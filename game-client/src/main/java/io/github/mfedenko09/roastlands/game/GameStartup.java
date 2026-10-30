package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.content.ContentLoader;
import io.github.mfedenko09.roastlands.core.content.GameContent;
import io.github.mfedenko09.roastlands.core.profile.LoadResult;
import io.github.mfedenko09.roastlands.core.profile.ProfileStore;
import io.github.mfedenko09.roastlands.core.session.ReviewSession;
import io.github.mfedenko09.roastlands.core.session.SessionLimits;
import java.time.LocalDate;

final class GameStartup {

    private GameStartup() {
    }

    static GameSession open(GamePaths paths, LocalDate today) {
        GameContent content = new ContentLoader().load(paths.contentDir());
        ProfileStore store = new ProfileStore(paths.profileDir(), content.topics().rootTopicIds());
        LoadResult loaded = store.load();
        ReviewSession session = ReviewSession.start(
                loaded.profile(), content.pack(), content.topics(), today, SessionLimits.DEFAULT);
        PanelFlow flow = new PanelFlow(session, store, content.topics(), loaded.restoredFromBackup());
        return new GameSession(flow, new ProgressView(content, today, loaded.profile()));
    }
}
