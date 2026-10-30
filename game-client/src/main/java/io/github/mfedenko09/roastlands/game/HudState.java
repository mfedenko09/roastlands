package io.github.mfedenko09.roastlands.game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.IconComponent;

class HudState extends BaseAppState {

    private static final float MARGIN = 16f;
    private static final float ABOVE_PANELS = 10f;
    private static final float ICON_GAP = 6f;

    private final PanelFlow flow;
    private final ProgressView progress;
    private final Label label = Widgets.text("", Widgets.TEXT, 0f);

    HudState(GameSession game) {
        this.flow = game.flow();
        this.progress = game.progress();
    }

    @Override
    protected void initialize(Application app) {
        flow.onAnswer(this::refresh);
        refresh();
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
        label.setIcon(null);
    }

    @Override
    public void update(float tpf) {
        label.setLocalTranslation(MARGIN, getApplication().getCamera().getHeight() - MARGIN, ABOVE_PANELS);
    }

    private void refresh() {
        TodayView today = progress.today(flow.progress());
        label.setText(today.text());
        label.setIcon(today.finished() ? checkIcon() : null);
    }

    private static IconComponent checkIcon() {
        IconComponent icon = new IconComponent(Widgets.CHECK_ICON);
        icon.setHAlignment(HAlignment.Right);
        icon.setMargin(ICON_GAP, 0f);
        return icon;
    }
}
