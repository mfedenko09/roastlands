package io.github.mfedenko09.roastlands.game;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

class InputBindings {

    private final InputManager inputManager;
    private final Map<Action, Integer> keys = new EnumMap<>(Map.ofEntries(
            Map.entry(Action.FORWARD, KeyInput.KEY_W),
            Map.entry(Action.BACK, KeyInput.KEY_S),
            Map.entry(Action.LEFT, KeyInput.KEY_A),
            Map.entry(Action.RIGHT, KeyInput.KEY_D),
            Map.entry(Action.RUN, KeyInput.KEY_LSHIFT),
            Map.entry(Action.JUMP, KeyInput.KEY_SPACE),
            Map.entry(Action.FULLSCREEN, KeyInput.KEY_F11),
            Map.entry(Action.INTERACT, KeyInput.KEY_E),
            Map.entry(Action.MAP, KeyInput.KEY_M),
            Map.entry(Action.OPTION_1, KeyInput.KEY_1),
            Map.entry(Action.OPTION_2, KeyInput.KEY_2),
            Map.entry(Action.OPTION_3, KeyInput.KEY_3),
            Map.entry(Action.OPTION_4, KeyInput.KEY_4),
            Map.entry(Action.OPTION_5, KeyInput.KEY_5),
            Map.entry(Action.SURE, KeyInput.KEY_S),
            Map.entry(Action.NOT_SURE, KeyInput.KEY_N),
            Map.entry(Action.CONFIRM, KeyInput.KEY_RETURN),
            Map.entry(Action.CANCEL, KeyInput.KEY_ESCAPE),
            Map.entry(Action.SCROLL_UP, KeyInput.KEY_UP),
            Map.entry(Action.SCROLL_DOWN, KeyInput.KEY_DOWN)));

    InputBindings(InputManager inputManager) {
        this.inputManager = inputManager;
        keys.forEach((action, key) -> inputManager.addMapping(action.name(), new KeyTrigger(key)));
    }

    void onPlayerAction(BiConsumer<Action, Boolean> listener) {
        ActionListener actionListener = (name, pressed, tpf) -> listener.accept(Action.valueOf(name), pressed);
        String[] playerActions = Action.PLAYER_CONTROLS.stream()
                .map(Action::name)
                .toArray(String[]::new);
        inputManager.addListener(actionListener, playerActions);
    }

    void onAction(Action action, Runnable onPressed) {
        ActionListener actionListener = (name, pressed, tpf) -> {
            if (pressed) {
                onPressed.run();
            }
        };
        inputManager.addListener(actionListener, action.name());
    }
}
