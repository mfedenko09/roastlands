package io.github.mfedenko09.roastlands.game;

import io.github.mfedenko09.roastlands.core.content.InvalidContentException;
import io.github.mfedenko09.roastlands.core.profile.ProfileCorruptedException;
import io.github.mfedenko09.roastlands.core.profile.UnsupportedProfileVersionException;
import io.github.mfedenko09.roastlands.core.topic.InvalidTopicMapException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

public final class Roastlands {

    private static final String ASSET_ROOT = "/assets/";

    private Roastlands() {
    }

    public static void main(String[] args) {
        List<String> missing = RequiredAssets.missing(Roastlands::existsOnClasspath);
        if (!missing.isEmpty()) {
            StartupErrorDialog.show(RequiredAssets.errorMessage(missing));
            return;
        }
        try {
            new RoastlandsApp(GameStartup.open(GamePaths.from(System::getProperty), LocalDate.now())).start();
        } catch (InvalidContentException | InvalidTopicMapException | ProfileCorruptedException
                | UnsupportedProfileVersionException | UncheckedIOException e) {
            StartupErrorDialog.show(ErrorReason.of(e));
        }
    }

    private static boolean existsOnClasspath(String assetPath) {
        return Roastlands.class.getResource(ASSET_ROOT + assetPath) != null;
    }
}
