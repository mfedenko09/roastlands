# Roastlands

A voxel-style learning game that keeps Java theory sharp. Islands are topics, questions come back on the day you
are about to forget them.

## Modules

| Module | Purpose |
|---|---|
| `knowledge-core` | Plain Java engine: SM-2, grading, mastery, daily queue, topic map, question loader, profile store |
| `content-tools` | Build-time tools for `content/`: topic map check and diagram, compile-and-run answer check |

`knowledge-core` has no game-engine or framework dependencies, so any client can reuse it.

## Content

`content/packs/<pack>/` holds `pack.yaml` and `topics.yaml`. Topics belong to islands and list their prerequisites,
which may point to topics of another pack. A topic opens when all its prerequisites have mastery of at least 0.6,
and an opened topic never closes again. The map is shown in [docs/topic-map.md](docs/topic-map.md).

Questions live in `content/packs/<pack>/questions/`, one file per topic. The format is described in
[content/README.md](content/README.md).

## Build

Requires JDK 21 (the Gradle toolchain picks it up).

```bash
./gradlew check
```

`check` also runs `verifyTopicMap`, which fails on a duplicate id, an unknown island or prerequisite, or a cycle.
After editing `topics.yaml`, regenerate the diagram:

```bash
./gradlew generateTopicMap
```

`check` also runs `verifyContent`, which compiles and runs the code of every question and fails when a marked answer
is not what the JVM really does. It lists all broken question ids with a reason.

`check` also runs Jacoco and fails when line or branch coverage drops below 90%.

## Player profile

`ProfileStore` keeps progress (card states, unlocked topics, streak, XP) in one JSON file with a `schemaVersion`.
The directory comes from `ProfilePaths.defaultDir()`: `%APPDATA%\Roastlands` on Windows, `~/.roastlands` elsewhere.

A save writes `profile.json.tmp`, flushes it to disk, copies the current file to `profile.json.bak` and moves the
temp file over `profile.json`, so a crash never leaves a half-written file. If `profile.json` is broken, `load()` uses
the backup and sets `restoredFromBackup`; if both are broken it throws `ProfileCorruptedException` and changes
nothing. A file saved by a newer game version is never read or overwritten.

## Daily queue contract

`DailyQueueBuilder` takes new card ids exactly as given. The caller must not pass ids that already have a card
state, otherwise such a card appears in the queue twice.

## Review session

`ReviewSession` runs one day of play on top of the profile, the content pack and the topic graph:

```java
ReviewSession session = ReviewSession.start(profile, content, topics, LocalDate.now(), SessionLimits.DEFAULT);
while (session.current().isPresent()) {
    AnswerResult result = session.answer(chosen, Confidence.HIGH);   // or answerTrueFalse(...)
    store.save(session.profile());                                   // the client saves after each answer
    session.next();
}
```

The queue is fixed at start: due cards (oldest first), then new questions from unlocked topics ordered by map
position, difficulty and id. A topic that opens during the session adds its questions from the next day.
`markTooEasy()` re-grades the last correct answer as EASY. The class is not thread-safe.
