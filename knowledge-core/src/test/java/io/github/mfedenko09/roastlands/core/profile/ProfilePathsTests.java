package io.github.mfedenko09.roastlands.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProfilePathsTests {

    private static final Path HOME = Path.of("/home/player");

    @Test
    void windowsUsesAppDataRoastlands() {
        assertThat(ProfilePaths.defaultDir("Windows 11", "/appdata", HOME)).isEqualTo(Path.of("/appdata/Roastlands"));
    }

    @Test
    void windowsWithoutAppDataFallsBackToHomeDir() {
        assertThat(ProfilePaths.defaultDir("Windows 11", null, HOME)).isEqualTo(HOME.resolve(".roastlands"));
        assertThat(ProfilePaths.defaultDir("Windows 11", " ", HOME)).isEqualTo(HOME.resolve(".roastlands"));
    }

    @Test
    void otherSystemsUseHiddenHomeDir() {
        assertThat(ProfilePaths.defaultDir("Linux", "/appdata", HOME)).isEqualTo(HOME.resolve(".roastlands"));
    }

    @Test
    void noArgumentOverloadReturnsAbsoluteRoastlandsDir() {
        assertThat(ProfilePaths.defaultDir()).isAbsolute().satisfiesAnyOf(
                dir -> assertThat(dir.getFileName()).hasToString("Roastlands"),
                dir -> assertThat(dir.getFileName()).hasToString(".roastlands"));
    }
}
