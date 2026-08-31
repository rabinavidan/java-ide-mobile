# Contributing to Java IDE Mobile

Thanks for considering a contribution. This project is a mobile Java IDE and (growing) interview
practice tool for Android — see [`README.md`](README.md) for what it does and
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for how it's built.

## Getting set up

Requirements: JDK 17, Android SDK (`compileSdk`/`targetSdk` 34, `minSdk` 26). A connected
device or emulator is only needed for instrumented tests and manual testing; everything else
(build, unit tests, lint) runs headless.

```bash
git clone <this repo>
cd java-ide-mobile
./gradlew assembleDebug        # confirms your toolchain works end to end
./gradlew testDebugUnitTest    # confirms the JVM test suite passes
```

## Before you open a PR

Run the checks CI runs, locally, first:

```bash
./gradlew testDebugUnitTest
./gradlew lint
./gradlew assembleDebug
```

If your change touches the compile/dex/run pipeline or the exercise catalog, also run (needs a
device/emulator):

```bash
./gradlew connectedAndroidTest
```

See [`docs/TESTING.md`](docs/TESTING.md) for what each test suite covers and how to run a single
test.

## Making changes

- **Adding a practice exercise?** Follow [`docs/ADDING_EXERCISES.md`](docs/ADDING_EXERCISES.md) —
  it walks through the exercise format, category placement, and the test coverage every exercise
  needs.
- **Touching the compiler/dexer/runner pipeline?** Read
  [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and [`docs/PRACTICE_ENGINE.md`](docs/PRACTICE_ENGINE.md)
  first — there are a few Android-specific workarounds baked into `JavaCompiler`/`Dexer` (see the
  inline comments in those files) that are easy to accidentally undo.
- **General code style:** match the surrounding file. Kotlin, no unnecessary abstraction, comments
  only where the *why* isn't obvious from the code (see `CLAUDE.md` for the fuller set of
  conventions this codebase follows).

## Commit and PR conventions

- Keep commits focused; a commit message should explain *why*, not just *what*.
- Don't remove or weaken an existing test to make CI pass — fix the underlying issue.
- If you're adding new practice content, make sure any reference solution you add actually
  compiles, dexes, and runs to produce the expected output — don't hand-type an expected output
  without verifying it (see `docs/ADDING_EXERCISES.md`).
- Open your PR against `main`. CI (`.github/workflows/ci.yml`) runs unit tests and instrumented
  tests automatically; both need to pass before merge.

## Reporting issues

Open a GitHub issue with: what you expected, what happened instead, and (for a compile/run bug)
the minimal Java snippet that reproduces it. If it's specific to a device/Android version, include
that too — the compile/dex/run pipeline runs in-process on-device and behavior can vary by API
level.

## Project scope note

This repository is mid-expansion from a small (30-exercise) practice catalog into a larger
interview-training platform — see [`docs/BASELINE.md`](docs/BASELINE.md) for the pre-expansion
baseline and the README's [Roadmap](README.md#roadmap) section for where it's headed. If you're
picking up a large chunk of that roadmap, consider opening an issue first to coordinate and avoid
duplicate work.
