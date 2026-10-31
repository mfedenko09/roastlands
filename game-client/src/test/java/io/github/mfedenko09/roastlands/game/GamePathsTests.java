package io.github.mfedenko09.roastlands.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mfedenko09.roastlands.core.profile.ProfilePaths;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GamePathsTests {

    @Test
    void usesDefaultsWhenNothingIsSet() {
        GamePaths paths = GamePaths.from(Map.<String, String>of()::get);

        assertThat(paths.contentDir()).isEmpty();
        assertThat(paths.profileDir()).isEqualTo(ProfilePaths.defaultDir());
    }

    @Test
    void propertiesOverrideTheDefaults() {
        GamePaths paths = GamePaths.from(Map.of("roastlands.content", "/c", "roastlands.profile", "/p")::get);

        assertThat(paths).isEqualTo(new GamePaths(Optional.of(Path.of("/c")), Path.of("/p")));
    }
}
