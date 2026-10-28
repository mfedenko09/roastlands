package io.github.mfedenko09.roastlands.game;

import com.jme3.system.AppSettings;

final class DisplaySettings {

    static final String TITLE = "Roastlands";
    static final int WINDOW_WIDTH = 1280;
    static final int WINDOW_HEIGHT = 720;

    private DisplaySettings() {
    }

    static AppSettings windowed() {
        AppSettings settings = base();
        settings.setResolution(WINDOW_WIDTH, WINDOW_HEIGHT);
        return settings;
    }

    static AppSettings fullscreen(int screenWidth, int screenHeight) {
        AppSettings settings = base();
        settings.setFullscreen(true);
        settings.setResolution(screenWidth, screenHeight);
        return settings;
    }

    private static AppSettings base() {
        AppSettings settings = new AppSettings(true);
        settings.setTitle(TITLE);
        settings.setVSync(true);
        settings.setFullscreen(false);
        return settings;
    }
}
