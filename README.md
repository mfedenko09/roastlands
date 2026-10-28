# Roastlands

A voxel-style learning game that keeps Java theory sharp. Islands are topics, questions come back on the day you
are about to forget them.

## Modules

| Module | Purpose |
|---|---|
| `knowledge-core` | Plain Java engine: SM-2 card scheduling, answer grading, topic mastery, daily queue |

`knowledge-core` has no game-engine or framework dependencies, so any client can reuse it.

## Build

Requires JDK 21 (the Gradle toolchain picks it up).

```bash
./gradlew check
```

`check` also runs Jacoco and fails when line or branch coverage drops below 90%.

## Daily queue contract

`DailyQueueBuilder` takes new card ids exactly as given. The caller must not pass ids that already have a card
state, otherwise such a card appears in the queue twice.
