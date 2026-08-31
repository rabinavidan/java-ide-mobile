# Java IDE Mobile

A Java IDE for Android that compiles, dexes and runs real Java code **entirely on-device** — no
server, no network call, no desktop companion. It doubles as a growing interview-practice tool:
a catalog of classic coding-interview exercises you can open, edit, run, and check against a
known-correct expected output, right on your phone.

> **Status:** actively evolving. This README describes what exists today. See
> [Roadmap](#roadmap) for where the project is headed — an expanded interview-training platform
> with Learn/Practice/Interview modes, 100 curated challenges, and progress tracking.

## Product purpose

Most "coding practice" apps either run your code on a remote server or don't run it at all —
they just check for exact syntax matches. Java IDE Mobile does neither: it embeds the actual
compiler and dex toolchain used by real Android builds (ECJ + D8) and executes your code
in-process on the device, so what you see is what an Android JVM would actually do.

Two things it's for:

1. **A real, general-purpose Java IDE on Android** — browse a project's files, edit Java with
   syntax highlighting and completion, compile, dex, and either run a console program or build
   and install a full Android APK, all without leaving the device.
2. **A pocket interview-practice tool** — a curated set of classic coding-interview exercises,
   each with a known-correct reference solution you can study, edit, and re-run to see if your
   changes still produce the right output.

## Main features

- On-device Java compilation (ECJ) and DEX conversion (D8) — no desktop, no CI, no network
- Multi-tab code editor (Sora Editor) with Java syntax highlighting, semantic completion, and
  inline diagnostics
- Run Java console programs and see captured stdout/stderr immediately
- Build, sign, and install full Android APKs from an on-device Android App project
- Practice catalog: browse coding-interview exercises by category, open a reference solution in
  the editor, edit it, and use **Run & Check** to compile/dex/run it and compare the output
  against the expected result
- Third-party dependency JARs auto-discovered from a project's `libs/` folder
- In-app Git support (JGit) including PGP-signed commits
- Project file search, package/class renaming, and cursor-position restoration across editor
  sessions

## Supported project types

| Type | What it does |
|---|---|
| **Java Console** | A plain Java program with a `public static void main(String[])`. Compiled, dexed, and run in-process; stdout/stderr captured and shown in-app. |
| **Android App** | A minimal Android app project (manifest + resources + Java sources). Resources are compiled, code is compiled and dexed, the result is packaged into a signed APK and installed on-device. |

## Java compilation flow

Both project types share the same core compile → dex pipeline; Android App projects add a
resource-compile step before it and a package/install step after.

**Java Console projects:**

```
JavaCompiler (ECJ)  →  .class files  →  Dexer (D8)  →  classes.dex  →  JavaRunner (DexClassLoader + reflection)
```

**Android App projects:**

```
ResourceCompiler (ARSCLib)  →  R.java + binary resources
        ↓
JavaCompiler (ECJ)  →  .class files
        ↓
Dexer (D8)  →  classes.dex
        ↓
Packager (apksig)  →  signed .apk
        ↓
ApkInstaller  →  installed on device
```

- **`JavaCompiler`** runs ECJ in-process against the project's `src/main/java` (plus any
  `libs/*.jar` on the classpath).
- **`Dexer`** runs D8 to convert `.class` files (and any bundled dependency jars) into a single
  `classes.dex`.
- **`JavaRunner`** loads `classes.dex` with `DexClassLoader`, finds the class with
  `public static void main(String[])`, and invokes it with `System.out`/`System.err` redirected
  into a capture buffer — meant for quick snippets, not sandboxed against a hostile or infinitely
  looping program.
- **`Packager`** signs the built APK with a debug key (`DebugSigningKey`, generated once and
  reused) via `apksig`.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full picture, including the UI flow
that drives this pipeline.

## Practice system

Today's practice system (`PracticeActivity` → `PracticeDetailActivity`) is a **worked-examples**
tool, not a blind quiz:

1. Open **Practice** from the main menu to see exercises grouped by category.
2. Tap an exercise to open it — the editor is pre-filled with a **known-correct reference
   solution** you can read, study, and edit.
3. Tap **Run & Check** — the current editor contents are compiled, dexed, and run through the same
   on-device pipeline described above, and the captured output is compared against the exercise's
   expected output.

This is intentionally transparent (you can see and tweak the reference solution) rather than a
hidden-answer quiz. See [`docs/PRACTICE_ENGINE.md`](docs/PRACTICE_ENGINE.md) for exactly how it
works today, its current constraints (single expected-output string, no hidden tests, no
difficulty levels or progress tracking yet), and where it's headed.

## Existing exercise categories

The catalog currently spans 10 categories (see [`docs/PRACTICE_ENGINE.md`](docs/PRACTICE_ENGINE.md)
for the full exercise list within each):

Fundamentals · Arrays & Strings · Linked Lists · Stacks & Queues · Trees · Graphs ·
Sorting & Searching · Recursion & Backtracking · Dynamic Programming · Bit Manipulation

In the app itself, this grouping is read live from `PracticeCategories.ALL` — the in-app UI never
hardcodes an exercise count or category list, so it stays accurate as the catalog grows. (This
README's category list above is a snapshot; the source of truth is
[`PracticeCategories.kt`](app/src/main/java/com/javaide/mobile/compiler/PracticeCategories.kt).)

## Screenshots

_Not yet included — tracked as a follow-up. Contributions welcome._

## Local build instructions

Requirements: JDK 17, Android SDK (`compileSdk`/`targetSdk` 34, `minSdk` 26).

```bash
# Debug APK
./gradlew assembleDebug

# Local unit tests (JVM, no device needed)
./gradlew testDebugUnitTest

# Single local test
./gradlew testDebugUnitTest --tests "com.javaide.mobile.compiler.InterviewExercisesCompileDexTest.fizzBuzz"

# Static analysis
./gradlew lint
```

The resulting debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Emulator requirements

Instrumented tests need a connected device or emulator. CI uses an API 29 x86_64 emulator
(`pixel_6` profile, 2048M RAM, 512M heap); locally, any device/emulator at `minSdk` 26 or above
works:

```bash
./gradlew connectedAndroidTest
# or, matching the CI script:
./ci/run-instrumented-tests.sh 29
```

## Test commands

See [`docs/TESTING.md`](docs/TESTING.md) for the full test-suite breakdown (unit vs. instrumented,
shared fixtures, Allure reporting). Quick reference:

```bash
./gradlew testDebugUnitTest        # JVM unit tests — fast, no device
./gradlew connectedAndroidTest      # instrumented tests — needs a device/emulator
./gradlew allureReport              # HTML report from local unit test results
```

## CI workflow

`.github/workflows/ci.yml` runs on every push/PR to `main`:

- **`unit-tests`** — `./gradlew testDebugUnitTest`, results uploaded as an artifact
- **`instrumented-tests`** — builds debug + androidTest APKs, boots an API 29 emulator, runs
  `ci/run-instrumented-tests.sh 29`, results uploaded as an artifact

## Project architecture

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full breakdown: UI flow, package
responsibilities, and a diagram of the practice flow (challenge selection → catalog →
editor → compiler → runner → evaluator → progress).

Single `:app` module, key packages:

| Package | Purpose |
|---|---|
| `com.javaide.mobile.compiler` | Compile/dex/run/build toolchain — `JavaCompiler`, `Dexer`, `JavaRunner`, `ResourceCompiler`, `Packager`, `ApkInstaller`, `AndroidJarProvider`, `PracticeCategories` |
| `com.javaide.mobile.ui` | Activities and RecyclerView adapters, including `PracticeActivity`/`PracticeDetailActivity` |
| `com.javaide.mobile.model` | `ProjectTemplate` — scaffolds Android App / Java Console projects |
| `com.javaide.mobile.util` | `ProjectStorage`, `FileOps`, `PackageRenamer`, `ClassRenamer`, `LogcatReader`, `ProjectSearch` |
| `com.javaide.mobile.completion` | `SemanticCompletionEngine`, `DiagnosticsEngine`, `ImportIndex`, `ImportInserter`, `DefinitionFinder` |
| `com.javaide.mobile.data` | Room database — `AppDatabase`, `AppDao`, `EditorState`, `EventEntry`, `Logger` |
| `com.javaide.mobile.vcs` | `GitRepository`, `GitCommitSigner`, `PgpKeyManager` |

## Roadmap

Java IDE Mobile is being expanded from a 30-exercise practice catalog into a structured
interview-training platform: 100 curated challenges (60 algorithms/data-structures, 20 Java-specific,
20 SDET/automation), with **Learn**, **Practice**, and timed **Interview** modes, hidden test
cases, hints, difficulty levels, and persistent progress tracking. See
[`docs/ROADMAP.md`](docs/ROADMAP.md) for the full, milestone-by-milestone plan (including
acceptance criteria and effort estimates), [`docs/BASELINE.md`](docs/BASELINE.md) for the
pre-expansion baseline this work builds from, and [`CONTRIBUTING.md`](CONTRIBUTING.md) for how to
get involved.

## Contribution instructions

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for dev setup and PR conventions, and
[`docs/ADDING_EXERCISES.md`](docs/ADDING_EXERCISES.md) for how to add a new practice exercise.

## Known limitations

- Practice checking is a single exact-string comparison against captured stdout — no hidden test
  cases, no partial credit
- No difficulty levels, hints, interview timer, or progress tracking yet
- No problem descriptions/constraints/examples — exercises are reference-solution-and-expected-output
  pairs, not full problem statements
- All 30 exercises live in one file (`InterviewExercises.kt`)
- `JavaRunner` executes practice code in-process with no timeout or sandbox — a hanging or
  crashing snippet can hang or crash the app
- No screenshots yet

See [`docs/BASELINE.md`](docs/BASELINE.md) for the full baseline snapshot this list is drawn from.

## License

No license file is currently included in this repository.
