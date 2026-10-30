package io.github.mfedenko09.roastlands.game;

import static io.github.mfedenko09.roastlands.game.Widgets.CONTENT_WIDTH;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import java.util.List;

class ProgressMapState extends BaseAppState {

    private static final String HINT = "Click an island or press Up/Down, M or Esc to close";
    private static final String EXPANDED = "[-] ";
    private static final String COLLAPSED = "[+] ";
    private static final String NEEDS_INDENT = "      ";
    private static final String COLUMN_GAP = "  ";
    private static final float TOP_MARGIN = 12f;
    private static final float LOCK_SLOT = 20f;
    private static final Insets3f PANEL_INSETS = new Insets3f(10f, 20f, 10f, 20f);
    private static final Insets3f ROW_INSETS = new Insets3f(0f, 10f, 0f, 0f);

    private final PanelFlow flow;
    private final ProgressView progress;
    private final InputBindings inputBindings;
    private final Container root = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));
    private BitmapFont monoFont;
    private List<IslandView> islands = List.of();
    private IslandAccordion accordion;

    ProgressMapState(GameSession game, InputBindings inputBindings) {
        this.flow = game.flow();
        this.progress = game.progress();
        this.inputBindings = inputBindings;
        setEnabled(false);
    }

    void toggle() {
        setEnabled(!isEnabled());
    }

    void close() {
        setEnabled(false);
    }

    @Override
    protected void initialize(Application app) {
        monoFont = app.getAssetManager().loadFont(Widgets.MONO_FONT);
        root.setBackground(new QuadBackgroundComponent(Widgets.PANEL));
        root.setInsets(PANEL_INSETS);
        inputBindings.onAction(Action.SCROLL_UP, () -> moveSection(-1));
        inputBindings.onAction(Action.SCROLL_DOWN, () -> moveSection(1));
    }

    @Override
    protected void onEnable() {
        getStateManager().getState(PlayerState.class).setFrozen(true);
        islands = progress.islands(flow.profile());
        accordion = IslandAccordion.openingFirstOpenIsland(islands);
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(root);
        render();
    }

    @Override
    protected void onDisable() {
        root.removeFromParent();
        PlayerState player = getStateManager().getState(PlayerState.class);
        if (player != null) {
            player.setFrozen(false);
        }
    }

    @Override
    protected void cleanup(Application app) {
        root.clearChildren();
    }

    private void moveSection(int delta) {
        if (isEnabled()) {
            accordion.move(delta);
            render();
        }
    }

    private void toggleSection(int index) {
        accordion.toggle(index);
        render();
    }

    private void render() {
        root.clearChildren();
        root.addChild(new Panel(Widgets.PANEL_WIDTH, 1f, ColorRGBA.BlackNoAlpha));
        int titleWidth = longestTitle();
        for (int index = 0; index < islands.size(); index++) {
            int island = index;
            renderHeader(island);
            if (accordion.isExpanded(island)) {
                islands.get(island).topics().forEach(topic -> renderTopic(topic, titleWidth));
            }
        }
        root.addChild(Widgets.text(HINT, Widgets.MUTED, CONTENT_WIDTH));
        place();
    }

    private int longestTitle() {
        return islands.stream()
                .flatMap(island -> island.topics().stream())
                .mapToInt(topic -> topic.title().length())
                .max()
                .orElse(0);
    }

    private void renderHeader(int index) {
        IslandView island = islands.get(index);
        String text = (accordion.isExpanded(index) ? EXPANDED : COLLAPSED) + island.title() + PanelHeader.SEPARATOR
                + island.summary();
        root.addChild(Widgets.button(text, Widgets.OPTION, CONTENT_WIDTH, () -> toggleSection(index)));
    }

    private void renderTopic(TopicView topic, int titleWidth) {
        Container row = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
        row.setBackground(null);
        row.setInsets(ROW_INSETS);
        row.addChild(topic.locked() ? lockIcon() : new Panel(LOCK_SLOT, 1f, ColorRGBA.BlackNoAlpha));
        row.addChild(Widgets.row(String.format(" %-" + titleWidth + "s", topic.title()), Widgets.TEXT, monoFont));
        row.addChild(Widgets.row(String.format(COLUMN_GAP + "%-7s", topic.level()), Widgets.colorOf(topic.level()),
                monoFont));
        row.addChild(Widgets.row(topic.progress(), Widgets.TEXT, monoFont));
        root.addChild(row);
        if (!topic.needs().isEmpty()) {
            root.addChild(Widgets.row(NEEDS_INDENT + topic.needs(), Widgets.MUTED, monoFont));
        }
    }

    private static Label lockIcon() {
        Label lock = new Label("");
        lock.setIcon(new IconComponent(Widgets.LOCK_ICON));
        return lock;
    }

    private void place() {
        SimpleApplication app = (SimpleApplication) getApplication();
        Vector3f size = root.getPreferredSize();
        float left = (app.getCamera().getWidth() - size.x) / 2f;
        root.setLocalTranslation(left, app.getCamera().getHeight() - TOP_MARGIN, 0f);
    }
}
