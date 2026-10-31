# Roastlands

A voxel-style learning game that keeps Java theory sharp. Islands are topics, questions come back on the day you
are about to forget them.

## Modules

| Module | Purpose |
|---|---|
| `knowledge-core` | Plain Java engine: SM-2, grading, mastery, daily queue, topic map, question loader, profile store |
| `content-tools` | Build-time tools for `content/`: topic map check and diagram, compile-and-run answer check |
| `cli` | Temporary console client: today's session and a mastery report in the terminal |
| `game-client` | jMonkeyEngine client: walk around the Core Shore island, answer questions at the terminal (Lemur UI) |

`knowledge-core` has no game-engine or framework dependencies, so any client can reuse it.

## Content

`content/packs/<pack>/` holds `pack.yaml` and `topics.yaml`. Topics belong to islands and list their prerequisites,
which may point to topics of another pack. A topic opens when all its prerequisites have mastery of at least 0.6,
and an opened topic never closes again. The map is shown in [docs/topic-map.md](docs/topic-map.md).

Questions live in `content/packs/<pack>/questions/`, one file per topic. The format is described in
[content/README.md](content/README.md).

## Play

```bash
./gradlew :game-client:run          # Windows: gradlew.bat :game-client:run
```

The game starts in fullscreen. WASD walks, Shift runs, Space jumps, the mouse turns the camera, F11 switches between
fullscreen and a window, Esc quits. Falling below y = -20 puts the character back at the spawn point.

Walk to the terminal (the cyan screen east of the spawn point) and press E within 2 m to open today's questions.
The character stops and the mouse cursor shows. Keys: 1-5 choose an option (checkboxes on multi-choice, Enter
submits), S "Sure" / N "Not sure", Enter next, Up/Down or mouse wheel scroll long code, Esc closes the panel and keeps
the session in memory. Progress is saved after every answer; a failed save shows a red "Progress not saved" line.

M opens the progress map: all islands with level, mastery and what each locked topic needs. Click an island header or
press Up/Down to open another section; M or Esc closes the map. The top-left corner always shows `Today done/total`,
then the level and the streak in days. A skipped day shows the streak as 0 until the next finished queue.

The game reads `content/` from the repo root (`:game-client:run` starts there) and the profile from
`ProfilePaths.defaultDir()`. Override with `-Droastlands.content=<dir>` and `-Droastlands.profile=<dir>`.

Rendering is not tested in CI; only the pure logic (movement, respawn, required-file check, question flow, progress
view) is.

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
position, difficulty and id. The limit of new questions is per day: cards first seen today
(`CardState.firstSeenDate`) are subtracted from it, so a second run on the same day only offers what is left.
Cards saved before `firstSeenDate` existed have no such date, so they never reduce the daily limit.
A topic that opens during the session adds its questions at the next start.
`markTooEasy()` re-grades the last correct answer as EASY. The class is not thread-safe.

## Console client

`cli` runs the daily session in a terminal on the same profile file as the future game.

```bash
./gradlew :cli:run --args="review"
./gradlew :cli:run --args="status"
```

`./gradlew :cli:installDist` builds `cli/build/install/roastlands-cli/bin/roastlands-cli` (and `.bat`) that takes the
same commands.

| Command | What it does |
|---|---|
| `review` | Asks today's questions, with "Sure? (y/n)" after each answer. Saves after every answer |
| `status` | Prints islands and topics with level, mastery %, locks and `Today done/total` |

| Option | Default |
|---|---|
| `--content <dir>` | `content` in the current folder (`:cli:run` starts in the repo root) |
| `--profile <dir>` | `ProfilePaths.defaultDir()` |

Colors for right and wrong answers are used only when the terminal is interactive and `TERM` or `WT_SESSION` is set.

`ProgressReporter` builds the state report (level, mastery, locks, `Today done/total`) that the clients show.

## Fonts

Code in the question panel uses a bitmap font generated from JetBrains Mono Regular at 18 px
(`assets/Interface/Fonts/JetBrainsMono-18.*`). JetBrains Mono is licensed under the SIL Open Font License 1.1, see
`JetBrainsMono-OFL.txt` next to the font. Neither this font nor the default one has a check mark or a lock, so both
are small icons in `assets/Interface/Icons/`.
