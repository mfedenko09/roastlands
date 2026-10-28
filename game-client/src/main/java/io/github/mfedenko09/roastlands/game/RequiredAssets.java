package io.github.mfedenko09.roastlands.game;

import java.util.List;
import java.util.function.Predicate;

final class RequiredAssets {

    static final List<String> PATHS = List.of(
            "Models/core-shore.obj", "Models/core-shore.mtl", "Models/character.obj", "Models/character.mtl");

    private RequiredAssets() {
    }

    static List<String> missing(Predicate<String> exists) {
        return PATHS.stream().filter(exists.negate()).toList();
    }

    static String errorMessage(List<String> missing) {
        return "Roastlands cannot start. Missing game files:\n" + String.join("\n", missing);
    }
}
