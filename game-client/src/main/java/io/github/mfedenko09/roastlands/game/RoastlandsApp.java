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

    private final PanelFlow flow;

    RoastlandsApp(PanelFlow flow) {
        this.flow = flow;
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
        QuestionPanelState panel = new QuestionPanelState(flow, inputBindings);
        inputBindings.onAction(Action.CANCEL, () -> cancel(panel));
        stateManager.attach(new IslandState());
        stateManager.attach(player);
        stateManager.attach(new TerminalState(inputBindings, panel));
        stateManager.attach(panel);
    }

    private void cancel(QuestionPanelState panel) {
        if (panel.isEnabled()) {
            panel.close();
        } else {
            stop();
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
