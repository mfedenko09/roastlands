package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.session.SessionProgress;

final class PanelHeader {

    private static final String SEPARATOR = "  |  ";

    private PanelHeader() {
    }

    static String text(String topicTitle, int difficulty, SessionProgress progress) {
        return topicTitle + SEPARATOR + "Difficulty " + difficulty + SEPARATOR + progress.done() + "/"
                + progress.total();
    }
}
