# Content format

A content pack lives in `content/packs/<pack>/`:

```
pack.yaml
topics.yaml                  # islands, topics and prerequisites, see docs/topic-map.md
questions/
  core.types.yaml            # one file per topic, the file name is the topic id
  core.strings.yaml
```

`pack.yaml` gives the pack `id`, `title`, `version` and `minJavaVersion`, which is the default for every question.

## Question

A question file is a YAML list of questions.

| Field | Required | Notes |
|---|---|---|
| `id` | yes | `<topic id>.<kebab-case-name>`, unique in all packs: `core.types.integer-cache` |
| `topic` | yes | must exist in the topic map and equal the file name |
| `type` | yes | `CODE_OUTPUT`, `SINGLE_CHOICE`, `MULTI_CHOICE`, `TRUE_FALSE` |
| `difficulty` | yes | 1 (junior) to 3 (senior interview) |
| `prompt` | yes | the question text |
| `code` | for `CODE_OUTPUT` | a full compilation unit with `public class Main` and `main`, imports visible |
| `options` | not for `TRUE_FALSE` | 2 to 5 different options |
| `correct` | yes | an index (`SINGLE_CHOICE`, `CODE_OUTPUT`), a list of indexes (`MULTI_CHOICE`), `true` or `false` |
| `explanation` | yes | why the answer is right and the popular wrong one is wrong, at most 600 characters |
| `reference` | yes | `JLS §14.20.2` or a `https://docs.oracle.com/` Javadoc URL |
| `minJavaVersion` | no | overrides the pack default |

Indexes start at 0. Unknown fields are rejected. `code` in a question that is not `CODE_OUTPUT` must still compile.

### CODE_OUTPUT

Options are outcomes, not free text. Exactly one of these forms per option:

| Option | Meaning |
|---|---|
| `output: "true\nfalse"` | exact stdout, line ends normalized to `\n`, one trailing `\n` removed |
| `compileError: true` | the code does not compile |
| `exception: java.lang.NullPointerException` | the program ends with this uncaught exception |
| `exception: ...` with `output: "..."` | the same, and this stdout was printed before the exception |

An `exception` option without `output` matches the exception whatever was printed before it. Because of that, the
verifier also rejects a question where another option matches the real outcome.

```yaml
- id: core.types.integer-cache
  topic: core.types
  type: CODE_OUTPUT
  difficulty: 1
  prompt: What does this code print?
  code: |
    public class Main {
        public static void main(String[] args) {
            Integer a = 127, b = 127;
            Integer c = 128, d = 128;
            System.out.println(a == b);
            System.out.println(c == d);
        }
    }
  options:
    - output: "true\ntrue"
    - output: "true\nfalse"
    - output: "false\nfalse"
    - compileError: true
  correct: 1
  explanation: >-
    Autoboxing calls Integer.valueOf, which must return cached objects for -128..127. 128 is outside the cache,
    so c and d are two different objects and == compares references.
  reference: JLS §5.1.7
```

### SINGLE_CHOICE

```yaml
- id: core.strings.concat-in-loop
  topic: core.strings
  type: SINGLE_CHOICE
  difficulty: 1
  prompt: Why is building a long text with s += part in a loop slow?
  options:
    - Each += creates a new String and copies all characters collected so far
    - The string pool must be searched for every new value
    - The JVM runs a garbage collection after every concatenation
  correct: 0
  explanation: >-
    String is immutable, so s += part allocates a new String and copies the old content into it.
  reference: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html
```

### MULTI_CHOICE

```yaml
- id: core.exceptions.checked-exceptions
  topic: core.exceptions
  type: MULTI_CHOICE
  difficulty: 1
  prompt: Which of these exception classes are checked exceptions?
  options:
    - java.io.IOException
    - java.lang.IllegalArgumentException
    - java.sql.SQLException
  correct: [0, 2]
  explanation: >-
    Checked exceptions extend Exception but not RuntimeException, so the compiler forces you to catch or declare them.
  reference: JLS §11.1.1
```

### TRUE_FALSE

No `options`.

```yaml
- id: core.object-contract.equal-hash-codes
  topic: core.object-contract
  type: TRUE_FALSE
  difficulty: 1
  prompt: >-
    True or false: if two objects have the same hashCode(), then equals() must return true for them.
  correct: false
  explanation: >-
    Equal objects must have equal hash codes, but different objects may share a hash code.
  reference: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#hashCode()
```

## Verification

`./gradlew verifyContent` (part of `./gradlew check`) loads every pack and checks the schema. It then compiles the
`code` of every question with `--release <minJavaVersion>`. For `CODE_OUTPUT` it runs `Main` in a separate process
three times with a timeout of 10 seconds and compares the real outcome with the marked option.
It fails when:

- the marked option is not the real outcome
- another option also matches the real outcome
- the outcome changes between the three runs (a data race, random values, time)
- the code runs longer than the timeout, or ends with a non-zero exit code and no uncaught exception

Every failure is one line `<file>:<question id>: <reason>`, and all failures are listed.

Rules for authors, which the verifier cannot check:

- Do not rely on unspecified order, for example the iteration order of `HashMap` or `HashSet`. It can differ between
  JDK versions.
- Test only one trick per question, and write distractors from the mistakes people really make.
- Do not copy questions from books or certification dumps. Use the JLS and the Javadoc as references.
