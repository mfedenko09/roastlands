package io.github.mfedenko09.roastlands.game;

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
        new RoastlandsApp().start();
    }

    private static boolean existsOnClasspath(String assetPath) {
        return Roastlands.class.getResource(ASSET_ROOT + assetPath) != null;
    }
}
