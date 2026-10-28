package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RequiredAssetsTests {

    @Test
    void nothingIsMissingWhenEveryFileExists() {
        assertThat(RequiredAssets.missing(path -> true)).isEmpty();
    }

    @Test
    void reportsEachMissingFile() {
        List<String> missing = RequiredAssets.missing(path -> !path.endsWith(".obj"));

        assertThat(missing).containsExactly("Models/core-shore.obj", "Models/character.obj");
    }

    @Test
    void reportsTheMissingCharacterModelToo() {
        List<String> missing = RequiredAssets.missing(path -> !path.startsWith("Models/character"));

        assertThat(missing).containsExactly("Models/character.obj", "Models/character.mtl");
    }

    @Test
    void errorMessageNamesTheMissingFile() {
        String message = RequiredAssets.errorMessage(List.of("Models/core-shore.obj"));

        assertThat(message).contains("Models/core-shore.obj");
    }

    @Test
    void shippedIslandModelIsOnTheClasspath() {
        assertThat(RequiredAssets.missing(path -> RequiredAssetsTests.class.getResource("/assets/" + path) != null))
                .isEmpty();
    }
}
