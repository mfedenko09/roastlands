package io.github.mfedenko09.roastlands.game;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

class RoastlandsApp extends SimpleApplication {

    private static final String ASSET_ROOT = "/assets";

    private final GameSession game;

    RoastlandsApp(GameSession game) {
        this.game = game;
        setShowSettings(false);
        setDisplayFps(false);
        setDisplayStatView(false);
        setSettings(DisplaySettings.windowed());
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator(ASSET_ROOT, ClasspathLocator.class);
        flyCam.setEnabled(false);
        GuiGlobals.initialize(this);
        inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        stateManager.attach(new BulletAppState());
        InputBindings inputBindings = new InputBindings(inputManager);
        PlayerState player = new PlayerState(inputBindings);
        inputBindings.onAction(Action.FULLSCREEN, this::toggleFullscreen);
        QuestionPanelState panel = new QuestionPanelState(game.flow(), inputBindings);
        ProgressMapState map = new ProgressMapState(game, inputBindings);
        inputBindings.onAction(Action.CANCEL, () -> cancel(panel, map));
        inputBindings.onAction(Action.MAP, () -> toggleMap(panel, map));
        stateManager.attach(new IslandState());
        stateManager.attach(player);
        stateManager.attach(new TerminalState(inputBindings, panel));
        stateManager.attach(panel);
        stateManager.attach(map);
        stateManager.attach(new HudState(game));
    }

    private void cancel(QuestionPanelState panel, ProgressMapState map) {
        if (panel.isEnabled()) {
            panel.close();
        } else if (map.isEnabled()) {
            map.close();
        } else {
            stop();
        }
    }

    private void toggleMap(QuestionPanelState panel, ProgressMapState map) {
        if (!panel.isEnabled()) {
            map.toggle();
        }
    }

    @Override
    public void loseFocus() {
        super.loseFocus();
        releasePlayerKeys();
    }

    @Override
    public void gainFocus() {
        super.gainFocus();
        releasePlayerKeys();
    }

    private void releasePlayerKeys() {
        PlayerState player = stateManager == null ? null : stateManager.getState(PlayerState.class);
        if (player != null) {
            player.releaseKeys();
        }
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
