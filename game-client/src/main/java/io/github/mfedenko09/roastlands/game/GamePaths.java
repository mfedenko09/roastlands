package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.profile.ProfilePaths;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

record GamePaths(Path contentDir, Path profileDir) {

    static final String CONTENT_PROPERTY = "roastlands.content";
    static final String PROFILE_PROPERTY = "roastlands.profile";

    private static final Path DEFAULT_CONTENT_DIR = Path.of("content");

    static GamePaths from(UnaryOperator<String> properties) {
        String content = properties.apply(CONTENT_PROPERTY);
        String profile = properties.apply(PROFILE_PROPERTY);
        return new GamePaths(
                content == null ? DEFAULT_CONTENT_DIR : Path.of(content),
                profile == null ? ProfilePaths.defaultDir() : Path.of(profile));
    }
}
