package io.github.mfedenko09.roastlands.game;

import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import io.github.mfedenko09.roastlands.core.mastery.MasteryLevel;

final class Widgets {

    static final String MONO_FONT = "Interface/Fonts/JetBrainsMono-18.fnt";
    static final float PANEL_WIDTH = 1000f;
    static final float CONTENT_WIDTH = PANEL_WIDTH - 40f;
    static final String LOCK_ICON = "Interface/Icons/lock.png";
    static final String CHECK_ICON = "Interface/Icons/check.png";
    static final String STREAK_ICON = "Interface/Icons/flame.png";
    static final ColorRGBA TEXT = ColorRGBA.White;
    static final ColorRGBA MUTED = new ColorRGBA(0.7f, 0.75f, 0.8f, 1f);
    static final ColorRGBA PANEL = new ColorRGBA(0.08f, 0.1f, 0.14f, 0.94f);
    static final ColorRGBA OPTION = new ColorRGBA(0.2f, 0.24f, 0.32f, 1f);
    static final ColorRGBA CHOSEN = new ColorRGBA(0.2f, 0.4f, 0.7f, 1f);
    static final ColorRGBA RIGHT = new ColorRGBA(0.15f, 0.5f, 0.25f, 1f);
    static final ColorRGBA WRONG = new ColorRGBA(0.65f, 0.18f, 0.18f, 1f);
    static final ColorRGBA ERROR_TEXT = new ColorRGBA(1f, 0.35f, 0.35f, 1f);
    static final ColorRGBA CODE_BACKGROUND = new ColorRGBA(0.03f, 0.04f, 0.06f, 1f);

    private static final float TEXT_SIZE = 20f;
    private static final float CODE_SIZE = 18f;
    private static final float ROW_SIZE = 15f;
    private static final ColorRGBA RUST = new ColorRGBA(0.9f, 0.5f, 0.25f, 1f);
    private static final ColorRGBA AMBER = new ColorRGBA(0.95f, 0.8f, 0.3f, 1f);
    private static final ColorRGBA MINT = new ColorRGBA(0.4f, 0.9f, 0.55f, 1f);
    private static final Insets3f BUTTON_INSETS = new Insets3f(5f, 10f, 5f, 10f);
    private static final Insets3f CODE_INSETS = new Insets3f(6f, 10f, 6f, 10f);

    private Widgets() {
    }

    static ColorRGBA colorOf(OptionMark mark) {
        return switch (mark) {
            case CORRECT -> RIGHT;
            case WRONG_CHOSEN -> WRONG;
            case CHOSEN -> CHOSEN;
            case NEUTRAL -> OPTION;
        };
    }

    static ColorRGBA colorOf(MasteryLevel level) {
        return switch (level) {
            case NEW -> MUTED;
            case RUSTY -> RUST;
            case WARM -> AMBER;
            case SHARP -> MINT;
        };
    }

    static Label text(String text, ColorRGBA color, float maxWidth) {
        Label label = new Label(text);
        label.setFontSize(TEXT_SIZE);
        label.setColor(color);
        label.setMaxWidth(maxWidth);
        return label;
    }

    static Label code(String text, BitmapFont monoFont) {
        Label label = new Label(text);
        label.setFont(monoFont);
        label.setFontSize(CODE_SIZE);
        label.setColor(TEXT);
        label.setBackground(new QuadBackgroundComponent(CODE_BACKGROUND));
        label.setInsets(CODE_INSETS);
        return label;
    }

    static Label row(String text, ColorRGBA color, BitmapFont monoFont) {
        Label label = new Label(text);
        label.setFont(monoFont);
        label.setFontSize(ROW_SIZE);
        label.setColor(color);
        return label;
    }

    static Button button(String text, ColorRGBA background, float maxWidth, Runnable onClick) {
        Button button = new Button(text);
        button.setFontSize(TEXT_SIZE);
        button.setColor(TEXT);
        button.setHighlightColor(ColorRGBA.Yellow);
        button.setMaxWidth(maxWidth);
        button.setBackground(new QuadBackgroundComponent(background));
        button.setInsets(BUTTON_INSETS);
        if (onClick != null) {
            button.addClickCommands(source -> onClick.run());
        }
        return button;
    }
}
