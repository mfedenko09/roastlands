package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.profile.ProfilePaths;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;

record GamePaths(Optional<Path> contentDir, Path profileDir) {

    static final String CONTENT_PROPERTY = "roastlands.content";
    static final String PROFILE_PROPERTY = "roastlands.profile";

    static GamePaths from(UnaryOperator<String> properties) {
        String profile = properties.apply(PROFILE_PROPERTY);
        return new GamePaths(
                Optional.ofNullable(properties.apply(CONTENT_PROPERTY)).map(Path::of),
                profile == null ? ProfilePaths.defaultDir() : Path.of(profile));
    }
}
