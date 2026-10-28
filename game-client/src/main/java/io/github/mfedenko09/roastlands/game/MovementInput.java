package io.github.mfedenko09.roastlands.game;

record MovementInput(boolean forward, boolean back, boolean left, boolean right, boolean run) {

    static final MovementInput NONE = new MovementInput(false, false, false, false, false);

    MovementInput with(Action action, boolean pressed) {
        return switch (action) {
            case FORWARD -> new MovementInput(pressed, back, left, right, run);
            case BACK -> new MovementInput(forward, pressed, left, right, run);
            case LEFT -> new MovementInput(forward, back, pressed, right, run);
            case RIGHT -> new MovementInput(forward, back, left, pressed, run);
            case RUN -> new MovementInput(forward, back, left, right, pressed);
            case JUMP, FULLSCREEN -> this;
        };
    }
}
