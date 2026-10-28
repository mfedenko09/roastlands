package io.github.mfedenko09.roastlands.core.profile;

public class UnsupportedProfileVersionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnsupportedProfileVersionException(int fileVersion, int supportedVersion) {
        super("Profile was saved by a newer version of the game: file has schemaVersion " + fileVersion
                + ", this game supports " + supportedVersion);
    }
}
