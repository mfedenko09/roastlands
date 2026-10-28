# Roastlands

A voxel-style learning game that keeps Java theory sharp. Islands are topics, questions come back on the day you
are about to forget them.

## Modules

| Module | Purpose |
|---|---|
| `knowledge-core` | Plain Java engine: SM-2, answer grading, mastery, daily queue, topic map, question loader |
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

## Daily queue contract

`DailyQueueBuilder` takes new card ids exactly as given. The caller must not pass ids that already have a card
state, otherwise such a card appears in the queue twice.
