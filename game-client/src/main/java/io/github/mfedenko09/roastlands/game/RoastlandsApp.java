package io.github.mfedenko09.roastlands.game;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.system.AppSettings;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

class RoastlandsApp extends SimpleApplication {

    private static final String ASSET_ROOT = "/assets";

    RoastlandsApp() {
        setShowSettings(false);
        setDisplayFps(false);
        setDisplayStatView(false);
        setSettings(DisplaySettings.windowed());
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator(ASSET_ROOT, ClasspathLocator.class);
        flyCam.setEnabled(false);
        stateManager.attach(new BulletAppState());
        InputBindings inputBindings = new InputBindings(inputManager);
        PlayerState player = new PlayerState(inputBindings);
        inputBindings.onAction(Action.FULLSCREEN, this::toggleFullscreen);
        stateManager.attach(new IslandState());
        stateManager.attach(player);
    }

    private void toggleFullscreen() {
        AppSettings current = context.getSettings();
        if (current.isFullscreen()) {
            restartWith(DisplaySettings.windowed());
            return;
        }
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode screen = device.getDisplayMode();
        restartWith(DisplaySettings.fullscreen(screen.getWidth(), screen.getHeight()));
    }

    private void restartWith(AppSettings settings) {
        setSettings(settings);
        restart();
    }
}
