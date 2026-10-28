package io.github.mfedenko09.roastlands.core.profile;

import java.nio.file.Path;

public class ProfileCorruptedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProfileCorruptedException(Path mainFile, Path backupFile) {
        super("Profile is corrupted: " + mainFile + " and " + backupFile + " cannot be read");
    }
}
