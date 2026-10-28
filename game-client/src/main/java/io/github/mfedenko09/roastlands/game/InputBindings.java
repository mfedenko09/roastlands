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
    private final Map<Action, Integer> keys = new EnumMap<>(Map.of(
            Action.FORWARD, KeyInput.KEY_W,
            Action.BACK, KeyInput.KEY_S,
            Action.LEFT, KeyInput.KEY_A,
            Action.RIGHT, KeyInput.KEY_D,
            Action.RUN, KeyInput.KEY_LSHIFT,
            Action.JUMP, KeyInput.KEY_SPACE,
            Action.FULLSCREEN, KeyInput.KEY_F11));

    InputBindings(InputManager inputManager) {
        this.inputManager = inputManager;
        keys.forEach((action, key) -> inputManager.addMapping(action.name(), new KeyTrigger(key)));
    }

    void onPlayerAction(BiConsumer<Action, Boolean> listener) {
        ActionListener actionListener = (name, pressed, tpf) -> listener.accept(Action.valueOf(name), pressed);
        String[] playerActions = keys.keySet().stream()
                .filter(action -> action != Action.FULLSCREEN)
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
