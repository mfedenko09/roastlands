package io.github.mfedenko09.roastlands.core.profile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mfedenko09.roastlands.core.srs.CardState;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileStoreTests {

    private static final Set<String> ROOTS = Set.of("core.types", "core.oop");

    private static final String HAND_WRITTEN = """
            {
              "schemaVersion": 1,
              "cards": [{ "cardId": "core.types.integer-cache", "repetitions": 2, "easeFactor": 2.5,
                          "intervalDays": 6, "dueDate": "2026-11-07", "lastReviewDate": "2026-11-01",
                          "lapses": 0 }],
              "unlockedTopics": ["core.types", "core.oop"],
              "streak": { "current": 5, "best": 9, "lastCompletedDate": "2026-11-01" },
              "xp": 120
            }
            """;

    @TempDir
    Path dir;

    private ProfileStore store(Path directory) {
        return new ProfileStore(directory, ROOTS);
    }

    @Test
    void firstRunReturnsEmptyProfileWithRootsUnlocked() {
        LoadResult result = store(dir).load();

        assertThat(result.restoredFromBackup()).isFalse();
        assertThat(result.profile().cards()).isEmpty();
        assertThat(result.profile().unlockedTopics()).containsExactlyInAnyOrder("core.types", "core.oop");
        assertThat(result.profile().streak()).isEqualTo(new Streak(0, 0, null));
        assertThat(result.profile().xp()).isZero();
        assertThat(result.profile().schemaVersion()).isEqualTo(1);
    }

    @Test
    void saveThenLoadReturnsEqualProfile() {
        PlayerProfile saved = profileWithCards("a", "b", "c").toBuilder()
                .unlockedTopics(Set.of("t1", "t2", "t3", "t4"))
                .streak(new Streak(5, 9, LocalDate.of(2026, 11, 1)))
                .xp(120)
                .build();
        ProfileStore store = store(dir);

        store.save(saved);
        LoadResult loaded = store.load();

        assertThat(loaded.profile()).isEqualTo(saved);
        assertThat(loaded.restoredFromBackup()).isFalse();
    }

    @Test
    void saveCreatesMissingDirectory() {
        Path nested = dir.resolve("a").resolve("b");

        store(nested).save(profileWithCards("a"));

        assertThat(nested.resolve("profile.json")).exists();
    }

    @Test
    void previousVersionBecomesBackup() throws IOException {
        ProfileStore store = store(dir);
        PlayerProfile first = profileWithCards("a");
        PlayerProfile second = profileWithCards("a", "b");

        store.save(first);
        store.save(second);

        assertThat(read("profile.json")).isEqualTo(contentOf(second));
        assertThat(read("profile.json.bak")).isEqualTo(contentOf(first));
        assertThat(dir.resolve("profile.json.tmp")).doesNotExist();
    }

    @Test
    void firstSaveCreatesNoBackup() {
        store(dir).save(profileWithCards("a"));

        assertThat(dir.resolve("profile.json.bak")).doesNotExist();
    }

    @Test
    void fileIsPrettyPrintedWithSortedCardsAndIsoDates() throws IOException {
        PlayerProfile profile = profileWithCards("b", "a").toBuilder().unlockedTopics(Set.of("z", "y")).build();

        store(dir).save(profile);

        String content = read("profile.json");
        assertThat(content).contains("\n  \"schemaVersion\" : 1,");
        assertThat(content.indexOf("\"cardId\" : \"a\"")).isLessThan(content.indexOf("\"cardId\" : \"b\""));
        assertThat(content.indexOf("\"y\"")).isLessThan(content.indexOf("\"z\""));
        assertThat(content).contains("\"dueDate\" : \"2026-11-07\"");
    }

    @Test
    void brokenMainFileIsRestoredFromBackup() throws IOException {
        ProfileStore store = store(dir);
        PlayerProfile good = profileWithCards("a");
        store.save(good);
        store.save(profileWithCards("a", "b"));
        Files.writeString(dir.resolve("profile.json"), "{ not json");

        LoadResult result = store.load();

        assertThat(result.restoredFromBackup()).isTrue();
        assertThat(result.profile()).isEqualTo(good);
    }

    @Test
    void missingMainFileIsRestoredFromBackup() throws IOException {
        ProfileStore store = store(dir);
        PlayerProfile good = profileWithCards("a");
        store.save(good);
        store.save(good);
        Files.delete(dir.resolve("profile.json"));

        LoadResult result = store.load();

        assertThat(result.restoredFromBackup()).isTrue();
        assertThat(result.profile()).isEqualTo(good);
    }

    @Test
    void bothFilesBrokenNamesBothPathsAndChangesNothing() throws IOException {
        Files.writeString(dir.resolve("profile.json"), "{ not json");
        Files.writeString(dir.resolve("profile.json.bak"), "");

        assertThatThrownBy(() -> store(dir).load())
                .isInstanceOf(ProfileCorruptedException.class)
                .hasMessageContaining(dir.resolve("profile.json").toString())
                .hasMessageContaining(dir.resolve("profile.json.bak").toString());

        assertThat(read("profile.json")).isEqualTo("{ not json");
        assertThat(read("profile.json.bak")).isEmpty();
    }

    @Test
    void brokenMainWithoutBackupIsCorrupted() throws IOException {
        Files.writeString(dir.resolve("profile.json"), "[]");

        assertThatThrownBy(() -> store(dir).load()).isInstanceOf(ProfileCorruptedException.class);
    }

    @Test
    void newerSchemaFailsEvenWhenBackupIsGood() throws IOException {
        ProfileStore store = store(dir);
        store.save(profileWithCards("a"));
        store.save(profileWithCards("a"));
        Files.writeString(dir.resolve("profile.json"), "{ \"schemaVersion\": 2, \"somethingNew\": true }");

        assertThatThrownBy(store::load)
                .isInstanceOf(UnsupportedProfileVersionException.class)
                .hasMessageContaining("Profile was saved by a newer version of the game");
    }

    @Test
    void saveRefusesToOverwriteProfileOfNewerVersion() throws IOException {
        String newer = "{ \"schemaVersion\": 2 }";
        Files.writeString(dir.resolve("profile.json"), newer);

        assertThatThrownBy(() -> store(dir).save(profileWithCards("a")))
                .isInstanceOf(UnsupportedProfileVersionException.class);

        assertThat(read("profile.json")).isEqualTo(newer);
        assertThat(dir.resolve("profile.json.tmp")).doesNotExist();
    }

    @Test
    void handWrittenFileLoadsIntoExpectedProfile() throws IOException {
        Files.writeString(dir.resolve("profile.json"), HAND_WRITTEN);

        PlayerProfile loaded = store(dir).load().profile();

        CardState card = CardState.builder()
                .cardId("core.types.integer-cache")
                .repetitions(2)
                .easeFactor(2.5)
                .intervalDays(6)
                .dueDate(LocalDate.of(2026, 11, 7))
                .lastReviewDate(LocalDate.of(2026, 11, 1))
                .lapses(0)
                .build();
        assertThat(loaded).isEqualTo(PlayerProfile.builder()
                .schemaVersion(1)
                .cards(Map.of(card.cardId(), card))
                .unlockedTopics(Set.of("core.types", "core.oop"))
                .streak(new Streak(5, 9, LocalDate.of(2026, 11, 1)))
                .xp(120)
                .build());
    }

    @Test
    void cardWithoutFirstSeenDateInTheFileLoadsWithNull() throws IOException {
        Files.writeString(dir.resolve("profile.json"), HAND_WRITTEN);

        CardState card = store(dir).load().profile().cards().get("core.types.integer-cache");

        assertThat(card.firstSeenDate()).isNull();
    }

    @Test
    void firstSeenDateIsWrittenAndReadBack() {
        PlayerProfile profile = profileWithCards("c1");

        store(dir).save(profile);

        assertThat(store(dir).load().profile().cards().get("c1").firstSeenDate())
                .isEqualTo(LocalDate.of(2026, 10, 30));
    }

    @Test
    void incompleteOrInconsistentFilesAreTreatedAsBroken() throws IOException {
        List<String> broken = List.of(
                "{ \"schemaVersion\": 1 }",
                "{ \"schemaVersion\": 0, \"cards\": [], \"unlockedTopics\": [], \"streak\": "
                        + "{\"current\":0,\"best\":0}, \"xp\": 0 }",
                "{ \"schemaVersion\": \"1\" }",
                "{ \"schemaVersion\": 1, \"cards\": [null], \"unlockedTopics\": [], \"streak\": "
                        + "{\"current\":0,\"best\":0}, \"xp\": 0 }",
                "{ \"schemaVersion\": 1, \"cards\": [{\"repetitions\":1}], \"unlockedTopics\": [], \"streak\": "
                        + "{\"current\":0,\"best\":0}, \"xp\": 0 }",
                "{ \"schemaVersion\": 1, \"cards\": [{\"cardId\":\"a\"},{\"cardId\":\"a\"}], \"unlockedTopics\": [], "
                        + "\"streak\": {\"current\":0,\"best\":0}, \"xp\": 0 }",
                "{ \"schemaVersion\": 1, \"cards\": [], \"unlockedTopics\": [], \"streak\": "
                        + "{\"current\":0,\"best\":0,\"lastCompletedDate\":\"yesterday\"}, \"xp\": 0 }");

        for (String content : broken) {
            Files.writeString(dir.resolve("profile.json"), content);
            assertThatThrownBy(() -> store(dir).load())
                    .as(content)
                    .isInstanceOf(ProfileCorruptedException.class);
        }
    }

    @Test
    void brokenMainIsNotCopiedOverGoodBackup() throws IOException {
        ProfileStore store = store(dir);
        PlayerProfile good = profileWithCards("a");
        store.save(good);
        store.save(good);
        Files.writeString(dir.resolve("profile.json"), "garbage");

        store.save(profileWithCards("a", "b"));

        assertThat(read("profile.json.bak")).isEqualTo(contentOf(good));
    }

    @Test
    void moveIsRetriedAndSucceedsOnThirdAttempt() {
        AtomicInteger attempts = new AtomicInteger();
        ProfileStore store = new ProfileStore(dir, ROOTS, flakyMover(attempts, 2), Duration.ZERO);

        store.save(profileWithCards("a"));

        assertThat(attempts).hasValue(3);
        assertThat(store.load().profile()).isEqualTo(profileWithCards("a"));
    }

    @Test
    void moveFailureAfterThreeAttemptsKeepsOldFileAndRemovesTemp() throws IOException {
        new ProfileStore(dir, ROOTS).save(profileWithCards("a"));
        String before = read("profile.json");
        AtomicInteger attempts = new AtomicInteger();
        ProfileStore store = new ProfileStore(dir, ROOTS, flakyMover(attempts, 99), Duration.ZERO);

        assertThatThrownBy(() -> store.save(profileWithCards("a", "b")))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining(dir.resolve("profile.json").toString());

        assertThat(attempts).hasValue(3);
        assertThat(read("profile.json")).isEqualTo(before);
        assertThat(dir.resolve("profile.json.tmp")).doesNotExist();
    }

    @Test
    void interruptedPauseStopsRetriesAndKeepsInterruptFlag() {
        AtomicInteger attempts = new AtomicInteger();
        FileMover interruptingMover = (from, to) -> {
            attempts.incrementAndGet();
            Thread.currentThread().interrupt();
            throw new AccessDeniedException(to.toString());
        };
        ProfileStore store = new ProfileStore(dir, ROOTS, interruptingMover, Duration.ofMillis(100));

        try {
            assertThatThrownBy(() -> store.save(profileWithCards("a"))).isInstanceOf(UncheckedIOException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        assertThat(attempts).hasValue(1);
    }

    @Test
    void saveFailsWhenDirectoryCannotBeCreated() throws IOException {
        Path file = Files.writeString(dir.resolve("not-a-dir"), "x");

        assertThatThrownBy(() -> store(file.resolve("child")).save(profileWithCards("a")))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void profileWithFiveThousandCardsRoundTrips() {
        String[] ids = IntStream.range(0, 5000).mapToObj(i -> "topic.card-" + i).toArray(String[]::new);
        PlayerProfile big = profileWithCards(ids);
        ProfileStore store = store(dir);

        store.save(big);

        assertThat(store.load().profile()).isEqualTo(big);
    }

    private static FileMover flakyMover(AtomicInteger attempts, int failures) {
        return (from, to) -> {
            if (attempts.incrementAndGet() <= failures) {
                throw new AccessDeniedException(to.toString());
            }
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        };
    }

    private String read(String name) throws IOException {
        return Files.readString(dir.resolve(name));
    }

    private static String contentOf(PlayerProfile profile) {
        try {
            return new String(new ProfileJson().write(profile), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static PlayerProfile profileWithCards(String... ids) {
        Map<String, CardState> cards = new HashMap<>();
        for (String id : ids) {
            cards.put(id, CardState.builder()
                    .cardId(id)
                    .repetitions(2)
                    .easeFactor(2.5)
                    .intervalDays(6)
                    .dueDate(LocalDate.of(2026, 11, 7))
                    .firstSeenDate(LocalDate.of(2026, 10, 30))
                    .lastReviewDate(LocalDate.of(2026, 11, 1))
                    .build());
        }
        return PlayerProfile.builder()
                .schemaVersion(1)
                .cards(cards)
                .unlockedTopics(ROOTS)
                .streak(Streak.none())
                .build();
    }
}
