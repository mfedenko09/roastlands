package io.github.mfedenko09.roastlands.game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Node;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.IconComponent;

class HudState extends BaseAppState {

    private static final float MARGIN = 16f;
    private static final float ABOVE_PANELS = 10f;
    private static final float ICON_GAP = 6f;
    private static final float STATS_GAP = 24f;

    private final PanelFlow flow;
    private final ProgressView progress;
    private final Label label = Widgets.text("", Widgets.TEXT, 0f);
    private final Label level = Widgets.text("", Widgets.TEXT, 0f);
    private final Label streak = Widgets.text("", Widgets.TEXT, 0f);

    HudState(GameSession game) {
        this.flow = game.flow();
        this.progress = game.progress();
    }

    @Override
    protected void initialize(Application app) {
        streak.setIcon(streakIcon());
        flow.onAnswer(this::refresh);
        refresh();
    }

    @Override
    protected void onEnable() {
        Node guiNode = ((SimpleApplication) getApplication()).getGuiNode();
        guiNode.attachChild(label);
        guiNode.attachChild(level);
        guiNode.attachChild(streak);
    }

    @Override
    protected void onDisable() {
        label.removeFromParent();
        level.removeFromParent();
        streak.removeFromParent();
    }

    @Override
    protected void cleanup(Application app) {
        label.setIcon(null);
        streak.setIcon(null);
    }

    @Override
    public void update(float tpf) {
        float top = getApplication().getCamera().getHeight() - MARGIN;
        label.setLocalTranslation(MARGIN, top, ABOVE_PANELS);
        float levelStart = MARGIN + label.getPreferredSize().x + STATS_GAP;
        level.setLocalTranslation(levelStart, top, ABOVE_PANELS);
        streak.setLocalTranslation(levelStart + level.getPreferredSize().x, top, ABOVE_PANELS);
    }

    private void refresh() {
        TodayView today = progress.today(flow.progress());
        label.setText(today.text());
        label.setIcon(today.finished() ? checkIcon() : null);
        PlayerStatsView stats = progress.stats(flow.profile());
        level.setText(stats.level());
        streak.setText(stats.streak());
    }

    private static IconComponent checkIcon() {
        IconComponent icon = new IconComponent(Widgets.CHECK_ICON);
        icon.setHAlignment(HAlignment.Right);
        icon.setMargin(ICON_GAP, 0f);
        return icon;
    }

    private static IconComponent streakIcon() {
        IconComponent icon = new IconComponent(Widgets.STREAK_ICON);
        icon.setMargin(ICON_GAP, 0f);
        return icon;
    }
}
