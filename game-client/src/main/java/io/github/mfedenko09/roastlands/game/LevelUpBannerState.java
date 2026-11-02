package io.github.mfedenko09.roastlands.game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import java.util.List;

class LevelUpBannerState extends BaseAppState {

    private static final float SHOWN_SECONDS = 3f;
    private static final float TOP_SHARE = 0.75f;
    private static final float ABOVE_PANELS = 20f;
    private static final Insets3f INSETS = new Insets3f(12f, 24f, 12f, 24f);

    private final PanelFlow flow;
    private final Label label = Widgets.text("", Widgets.TEXT, 0f);
    private float secondsLeft;
    private boolean firstFrame;

    LevelUpBannerState(PanelFlow flow) {
        this.flow = flow;
        setEnabled(false);
    }

    void show() {
        List<Integer> levelUps = flow.takeLevelUps();
        if (levelUps.isEmpty()) {
            return;
        }
        label.setText(LevelUpBanner.text(levelUps));
        secondsLeft = SHOWN_SECONDS;
        firstFrame = true;
        setEnabled(true);
    }

    @Override
    protected void initialize(Application app) {
        label.setBackground(new QuadBackgroundComponent(Widgets.PANEL));
        label.setInsets(INSETS);
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(label);
    }

    @Override
    protected void onDisable() {
        label.removeFromParent();
    }

    @Override
    protected void cleanup(Application app) {
        label.setBackground(null);
    }

    @Override
    public void update(float tpf) {
        if (firstFrame) {
            firstFrame = false;
        } else {
            secondsLeft -= tpf;
        }
        if (secondsLeft <= 0f) {
            setEnabled(false);
            return;
        }
        float x = (getApplication().getCamera().getWidth() - label.getPreferredSize().x) / 2f;
        float y = getApplication().getCamera().getHeight() * TOP_SHARE;
        label.setLocalTranslation(x, y, ABOVE_PANELS);
    }
}
