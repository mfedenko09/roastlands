package io.github.mfedenko09.roastlands.core.profile;

import java.nio.file.Path;
import java.util.Locale;

public final class ProfilePaths {

    private static final String WINDOWS_DIR = "Roastlands";
    private static final String UNIX_DIR = ".roastlands";

    private ProfilePaths() {}

    public static Path defaultDir() {
        Path home = Path.of(System.getProperty("user.home"));
        return defaultDir(System.getProperty("os.name"), System.getenv("APPDATA"), home);
    }

    static Path defaultDir(String osName, String appData, Path home) {
        boolean windows = osName.toLowerCase(Locale.ROOT).startsWith("windows");
        if (windows && appData != null && !appData.isBlank()) {
            return Path.of(appData).resolve(WINDOWS_DIR);
        }
        return home.resolve(UNIX_DIR);
    }
}
