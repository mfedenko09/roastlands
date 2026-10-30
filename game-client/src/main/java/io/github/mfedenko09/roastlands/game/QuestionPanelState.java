package io.github.mfedenko09.roastlands.game;

import static io.github.mfedenko09.roastlands.game.Widgets.CONTENT_WIDTH;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import io.github.mfedenko09.roastlands.core.answer.Confidence;
import io.github.mfedenko09.roastlands.core.content.Question;
import io.github.mfedenko09.roastlands.core.session.AnswerResult;
import java.util.List;

class QuestionPanelState extends BaseAppState {

    private static final String WHEEL_UP = "PANEL_WHEEL_UP";
    private static final String WHEEL_DOWN = "PANEL_WHEEL_DOWN";
    private static final float TOP_MARGIN = 12f;
    private static final float HALF_ROW_WIDTH = CONTENT_WIDTH / 2f - 10f;
    private static final int VISIBLE_CODE_LINES = 10;
    private static final Insets3f PANEL_INSETS = new Insets3f(10f, 20f, 10f, 20f);

    private final PanelFlow flow;
    private final InputBindings inputBindings;
    private final Container root = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));
    private BitmapFont monoFont;
    private Question shownQuestion;
    private CodeWindow codeWindow;

    QuestionPanelState(PanelFlow flow, InputBindings inputBindings) {
        this.flow = flow;
        this.inputBindings = inputBindings;
        setEnabled(false);
    }

    void open() {
        setEnabled(true);
    }

    void close() {
        setEnabled(false);
    }

    @Override
    protected void initialize(Application app) {
        monoFont = app.getAssetManager().loadFont(Widgets.MONO_FONT);
        root.setBackground(new QuadBackgroundComponent(Widgets.PANEL));
        root.setInsets(PANEL_INSETS);
        bindKeys();
        bindWheel(app);
    }

    @Override
    protected void onEnable() {
        getStateManager().getState(PlayerState.class).setFrozen(true);
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

    private void bindKeys() {
        for (Action action : Action.values()) {
            action.optionIndex().ifPresent(
                    index -> inputBindings.onAction(action, () -> whenOpen(() -> flow.pick(index))));
        }
        inputBindings.onAction(Action.SURE, () -> whenOpen(() -> flow.answer(Confidence.HIGH)));
        inputBindings.onAction(Action.NOT_SURE, () -> whenOpen(() -> flow.answer(Confidence.LOW)));
        inputBindings.onAction(Action.CONFIRM, () -> whenOpen(flow::confirm));
        inputBindings.onAction(Action.SCROLL_UP, () -> whenOpen(() -> scrollCode(-1)));
        inputBindings.onAction(Action.SCROLL_DOWN, () -> whenOpen(() -> scrollCode(1)));
    }

    private void bindWheel(Application app) {
        app.getInputManager().addMapping(WHEEL_UP, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        app.getInputManager().addMapping(WHEEL_DOWN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        AnalogListener wheel = (name, value, tpf) -> {
            if (isEnabled()) {
                scrollCode(WHEEL_UP.equals(name) ? -1 : 1);
                render();
            }
        };
        app.getInputManager().addListener(wheel, WHEEL_UP, WHEEL_DOWN);
    }

    private void whenOpen(Runnable change) {
        if (isEnabled()) {
            apply(change);
        }
    }

    private void apply(Runnable change) {
        change.run();
        render();
    }

    private void scrollCode(int delta) {
        if (codeWindow != null) {
            codeWindow.scroll(delta);
        }
    }

    private void render() {
        root.clearChildren();
        root.addChild(new Panel(Widgets.PANEL_WIDTH, 1f, ColorRGBA.BlackNoAlpha));
        flow.notice().ifPresent(notice -> root.addChild(Widgets.text(notice, ColorRGBA.Yellow, CONTENT_WIDTH)));
        if (flow.step() == PanelStep.DONE) {
            renderDone();
        } else {
            renderQuestion();
        }
        flow.saveError().ifPresent(error -> root.addChild(Widgets.text(error, Widgets.ERROR_TEXT, CONTENT_WIDTH)));
        place();
    }

    private void renderQuestion() {
        if (codeWindow == null || !flow.question().equals(shownQuestion)) {
            shownQuestion = flow.question();
            codeWindow = new CodeWindow(shownQuestion.code(), VISIBLE_CODE_LINES);
        }
        root.addChild(Widgets.text(flow.header(), Widgets.MUTED, CONTENT_WIDTH));
        root.addChild(Widgets.text(shownQuestion.prompt(), Widgets.TEXT, CONTENT_WIDTH));
        renderCode();
        renderOptions();
        switch (flow.step()) {
            case CHOOSING -> renderChoosingHint();
            case CONFIDENCE -> renderConfidence();
            case RESULT -> renderResult();
            case DONE -> throw new IllegalStateException("no question after the last answer");
        }
    }

    private void renderCode() {
        if (codeWindow.totalLines() == 0) {
            return;
        }
        root.addChild(Widgets.code(String.join("\n", codeWindow.visibleLines()), monoFont));
        if (codeWindow.isScrollable()) {
            String position = "Lines " + codeWindow.firstLineNumber() + "-" + codeWindow.lastLineNumber() + " of "
                    + codeWindow.totalLines() + " (mouse wheel or Up/Down to scroll)";
            root.addChild(Widgets.text(position, Widgets.MUTED, CONTENT_WIDTH));
        }
    }

    private void renderOptions() {
        List<String> labels = flow.optionLabels();
        for (int index = 0; index < labels.size(); index++) {
            int optionIndex = index;
            String text = optionPrefix(index) + labels.get(index);
            Runnable onClick = flow.canPick() ? () -> apply(() -> flow.pick(optionIndex)) : null;
            root.addChild(Widgets.button(text, Widgets.colorOf(flow.markOf(index)), CONTENT_WIDTH, onClick));
        }
    }

    private String optionPrefix(int index) {
        String box = flow.isMultiChoice() ? (flow.chosen().contains(index) ? "[x] " : "[ ] ") : "";
        return box + (index + 1) + ") ";
    }

    private void renderChoosingHint() {
        if (flow.isMultiChoice()) {
            root.addChild(Widgets.button("Submit (Enter)", Widgets.CHOSEN, CONTENT_WIDTH, () -> apply(flow::confirm)));
        }
        root.addChild(Widgets.text("Keys: 1-5 choose, Esc close", Widgets.MUTED, CONTENT_WIDTH));
    }

    private void renderConfidence() {
        root.addChild(Widgets.text("How sure are you?", Widgets.TEXT, CONTENT_WIDTH));
        Container row = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
        row.setBackground(null);
        row.addChild(Widgets.button("Sure (S)", Widgets.RIGHT, HALF_ROW_WIDTH, answering(Confidence.HIGH)));
        row.addChild(Widgets.button("Not sure (N)", Widgets.OPTION, HALF_ROW_WIDTH, answering(Confidence.LOW)));
        root.addChild(row);
    }

    private Runnable answering(Confidence confidence) {
        return () -> apply(() -> flow.answer(confidence));
    }

    private void renderResult() {
        AnswerResult result = flow.result();
        root.addChild(result.correct()
                ? Widgets.text("Correct!", ColorRGBA.Green, CONTENT_WIDTH)
                : Widgets.text("Wrong.", Widgets.ERROR_TEXT, CONTENT_WIDTH));
        root.addChild(Widgets.text(result.explanation(), Widgets.TEXT, CONTENT_WIDTH));
        if (result.reference() != null && !result.reference().isBlank()) {
            root.addChild(Widgets.text("Reference: " + result.reference(), Widgets.MUTED, CONTENT_WIDTH));
        }
        if (!result.topicsUnlocked().isEmpty()) {
            root.addChild(Widgets.text("Unlocked: " + String.join(", ", flow.titles(result.topicsUnlocked())),
                    ColorRGBA.Yellow, CONTENT_WIDTH));
        }
        Container row = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
        row.setBackground(null);
        row.addChild(Widgets.button("Next (Enter)", Widgets.CHOSEN, HALF_ROW_WIDTH, () -> apply(flow::confirm)));
        if (flow.canMarkTooEasy()) {
            row.addChild(Widgets.button("Too easy", Widgets.OPTION, HALF_ROW_WIDTH, () -> apply(flow::markTooEasy)));
        }
        root.addChild(row);
    }

    private void renderDone() {
        DaySummary summary = flow.summary();
        if (summary.answered() == 0) {
            root.addChild(Widgets.text("Nothing is due today.", Widgets.TEXT, CONTENT_WIDTH));
        } else {
            root.addChild(Widgets.text("Done for today", ColorRGBA.Green, CONTENT_WIDTH));
            root.addChild(Widgets.text("Answered: " + summary.answered(), Widgets.TEXT, CONTENT_WIDTH));
            root.addChild(Widgets.text("Correct: " + summary.correctPercent() + "%", Widgets.TEXT, CONTENT_WIDTH));
            String unlocked = summary.topicsUnlocked().isEmpty()
                    ? "none" : String.join(", ", flow.titles(summary.topicsUnlocked()));
            root.addChild(Widgets.text("Topics unlocked: " + unlocked, Widgets.TEXT, CONTENT_WIDTH));
        }
        root.addChild(Widgets.button("Close (Esc)", Widgets.CHOSEN, CONTENT_WIDTH, this::close));
    }

    private void place() {
        SimpleApplication app = (SimpleApplication) getApplication();
        Vector3f size = root.getPreferredSize();
        float left = (app.getCamera().getWidth() - size.x) / 2f;
        root.setLocalTranslation(left, app.getCamera().getHeight() - TOP_MARGIN, 0f);
    }
}
