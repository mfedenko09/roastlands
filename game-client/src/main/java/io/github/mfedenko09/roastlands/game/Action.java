package io.github.mfedenko09.roastlands.game;

import java.util.EnumSet;
import java.util.OptionalInt;
import java.util.Set;

enum Action {
    FORWARD,
    BACK,
    LEFT,
    RIGHT,
    RUN,
    JUMP,
    FULLSCREEN,
    INTERACT,
    MAP,
    OPTION_1,
    OPTION_2,
    OPTION_3,
    OPTION_4,
    OPTION_5,
    SURE,
    NOT_SURE,
    CONFIRM,
    CANCEL,
    SCROLL_UP,
    SCROLL_DOWN;

    static final Set<Action> PLAYER_CONTROLS = EnumSet.of(FORWARD, BACK, LEFT, RIGHT, RUN, JUMP);

    OptionalInt optionIndex() {
        return switch (this) {
            case OPTION_1 -> OptionalInt.of(0);
            case OPTION_2 -> OptionalInt.of(1);
            case OPTION_3 -> OptionalInt.of(2);
            case OPTION_4 -> OptionalInt.of(3);
            case OPTION_5 -> OptionalInt.of(4);
            default -> OptionalInt.empty();
        };
    }
}
