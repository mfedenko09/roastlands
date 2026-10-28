package io.github.mfedenko09.roastlands.core.profile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public final class ProfileStore {

    static final int MOVE_ATTEMPTS = 3;
    static final Duration MOVE_RETRY_PAUSE = Duration.ofMillis(100);

    private static final String PROFILE_FILE = "profile.json";
    private static final String BACKUP_SUFFIX = ".bak";
    private static final String TEMP_SUFFIX = ".tmp";

    private final Path dir;
    private final Path mainFile;
    private final Path backupFile;
    private final Path tempFile;
    private final Set<String> rootTopics;
    private final FileMover mover;
    private final Duration retryPause;
    private final ProfileJson json = new ProfileJson();

    public ProfileStore(Path dir, Set<String> rootTopics) {
        this(dir, rootTopics, ProfileStore::moveAtomically, MOVE_RETRY_PAUSE);
    }

    ProfileStore(Path dir, Set<String> rootTopics, FileMover mover, Duration retryPause) {
        this.dir = dir;
        this.mainFile = dir.resolve(PROFILE_FILE);
        this.backupFile = dir.resolve(PROFILE_FILE + BACKUP_SUFFIX);
        this.tempFile = dir.resolve(PROFILE_FILE + TEMP_SUFFIX);
        this.rootTopics = Set.copyOf(rootTopics);
        this.mover = mover;
        this.retryPause = retryPause;
    }

    public LoadResult load() {
        if (!Files.exists(mainFile) && !Files.exists(backupFile)) {
            return new LoadResult(PlayerProfile.empty(rootTopics), false);
        }
        Optional<PlayerProfile> fromMain = json.read(mainFile);
        if (fromMain.isPresent()) {
            return new LoadResult(fromMain.get(), false);
        }
        PlayerProfile fromBackup = json.read(backupFile)
                .orElseThrow(() -> new ProfileCorruptedException(mainFile, backupFile));
        return new LoadResult(fromBackup, true);
    }

    public void save(PlayerProfile profile) {
        try {
            Files.createDirectories(dir);
            writeTempFile(profile);
            keepCurrentAsBackup();
            moveWithRetry();
        } catch (IOException e) {
            deleteTempQuietly();
            throw new UncheckedIOException("Cannot save profile to " + mainFile, e);
        } catch (RuntimeException e) {
            deleteTempQuietly();
            throw e;
        }
    }

    private void writeTempFile(PlayerProfile profile) throws IOException {
        byte[] content = json.write(profile);
        try (FileChannel channel = FileChannel.open(
                tempFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(content));
            channel.force(true);
        }
    }

    private void keepCurrentAsBackup() throws IOException {
        if (json.read(mainFile).isPresent()) {
            Files.copy(mainFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void moveWithRetry() throws IOException {
        for (int attempt = 1; ; attempt++) {
            try {
                mover.move(tempFile, mainFile);
                return;
            } catch (IOException e) {
                if (attempt == MOVE_ATTEMPTS) {
                    throw e;
                }
                pauseBeforeRetry(e);
            }
        }
    }

    private void pauseBeforeRetry(IOException cause) throws IOException {
        try {
            Thread.sleep(retryPause);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw cause;
        }
    }

    private void deleteTempQuietly() {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
        }
    }

    private static void moveAtomically(Path from, Path to) throws IOException {
        Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
