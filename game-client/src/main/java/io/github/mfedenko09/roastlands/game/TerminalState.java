package io.github.mfedenko09.roastlands.game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.simsilica.lemur.Label;

class TerminalState extends BaseAppState {

    private static final float TERMINAL_X = 5f;
    private static final float TERMINAL_Z = 0f;
    private static final float HINT_BOTTOM = 90f;
    private static final float HINT_SIZE = 22f;
    private static final String HINT = "Press E to answer today's questions";
    private static final ColorRGBA STAND = new ColorRGBA(0.35f, 0.38f, 0.45f, 1f);
    private static final ColorRGBA SCREEN = new ColorRGBA(0.2f, 0.85f, 0.75f, 1f);

    private final InputBindings inputBindings;
    private final QuestionPanelState panel;
    private final Node terminal = new Node("terminal");
    private final Label hint = new Label(HINT);

    TerminalState(InputBindings inputBindings, QuestionPanelState panel) {
        this.inputBindings = inputBindings;
        this.panel = panel;
    }

    @Override
    protected void initialize(Application app) {
        terminal.attachChild(block(app, new Vector3f(0.4f, 0.5f, 0.4f), STAND, 0.5f));
        terminal.attachChild(block(app, new Vector3f(0.6f, 0.4f, 0.1f), SCREEN, 1.4f));
        float ground = getStateManager().getState(IslandState.class).groundHeightAt(TERMINAL_X, TERMINAL_Z);
        terminal.setLocalTranslation(TERMINAL_X, ground, TERMINAL_Z);
        hint.setFontSize(HINT_SIZE);
        hint.setColor(ColorRGBA.White);
        inputBindings.onAction(Action.INTERACT, this::interact);
    }

    @Override
    protected void onEnable() {
        SimpleApplication app = (SimpleApplication) getApplication();
        app.getRootNode().attachChild(terminal);
    }

    @Override
    protected void onDisable() {
        hint.removeFromParent();
        terminal.removeFromParent();
    }

    @Override
    protected void cleanup(Application app) {
        terminal.detachAllChildren();
    }

    @Override
    public void update(float tpf) {
        boolean show = isNearTerminal() && !panel.isEnabled();
        if (show && hint.getParent() == null) {
            ((SimpleApplication) getApplication()).getGuiNode().attachChild(hint);
        } else if (!show) {
            hint.removeFromParent();
        }
        if (show) {
            Vector3f size = hint.getPreferredSize();
            hint.setLocalTranslation(
                    (getApplication().getCamera().getWidth() - size.x) / 2f, HINT_BOTTOM + size.y, 0f);
        }
    }

    private void interact() {
        if (isEnabled() && isNearTerminal() && !panel.isEnabled()) {
            panel.open();
        }
    }

    private boolean isNearTerminal() {
        Vector3f player = getStateManager().getState(PlayerState.class).position();
        return TerminalZone.contains(terminal.getWorldTranslation(), player);
    }

    private static Geometry block(Application app, Vector3f halfExtents, ColorRGBA color, float centerY) {
        Geometry geometry = new Geometry("terminal-part", new Box(halfExtents.x, halfExtents.y, halfExtents.z));
        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        geometry.setMaterial(material);
        geometry.setLocalTranslation(0f, centerY, 0f);
        return geometry;
    }
}
