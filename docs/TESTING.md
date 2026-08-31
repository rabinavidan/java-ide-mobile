# Testing

## Test suites

| Location | Runs on | Command |
|---|---|---|
| `app/src/test/` | JVM (no device) | `./gradlew testDebugUnitTest` |
| `app/src/androidTest/` | Connected device/emulator | `./gradlew connectedAndroidTest` |
| `app/src/testShared/` | N/A — compiled into `main`, not a test suite itself | — |

### `app/src/test/` — JVM unit tests

Fast, no device needed. Covers pure logic and anything that can run against a plain JVM plus a
provided `android.jar` (see below) — the compiler/dexer, string/file utilities, completion engine,
project templates, etc.

```bash
./gradlew testDebugUnitTest

# Single test class
./gradlew testDebugUnitTest --tests "com.javaide.mobile.compiler.JavaCompilerTest"

# Single test method
./gradlew testDebugUnitTest --tests "com.javaide.mobile.compiler.InterviewExercisesCompileDexTest.fizzBuzz"
```

`InterviewExercisesCompileDexTest` specifically verifies the compile→dex half of the pipeline
(ECJ + D8) against every catalog exercise's *unmodified* reference solution — it can't verify
*execution* on a plain JVM (no `DexClassLoader` outside Android), which is what the instrumented
counterpart below is for.

**`android.jar` availability:** local unit tests need a real `android.jar` on the classpath (for
ECJ/D8 to compile/dex against). `app/build.gradle.kts` sets this as a system property automatically:

```kotlin
systemProperty("android.jar.path", provider { android.bootClasspath.first().absolutePath }.get())
```

read by test setup code via `System.getProperty("android.jar.path")` — nothing to configure
manually.

### `app/src/androidTest/` — instrumented tests

Needs a connected device or emulator (`minSdk` 26+). Covers anything that needs the real Android
runtime — `DexClassLoader` execution, UI interaction (Espresso), on-device file storage.

```bash
./gradlew connectedAndroidTest
```

`InterviewExercisesRunTest` is the execution counterpart to `InterviewExercisesCompileDexTest`:
compiles, dexes, **and runs** every catalog exercise, asserting the captured output matches
`expectedOutput` exactly. This is effectively a regression guard for the whole practice catalog —
if it fails, either a reference solution broke or its expected output is wrong.

UI tests (`ui/FlatViewTest`, `ui/SearchInEditorTest`, `ui/FileExplorerNavigationTest`,
`ui/EditorTabsTest`, `ui/RunFromEditorConsoleTest`) drive real Activities with Espresso.

### `app/src/testShared/` — shared fixtures

Contains `InterviewExercises.kt` (the practice catalog — see
[`PRACTICE_ENGINE.md`](PRACTICE_ENGINE.md)). Added to the **`main`** sourceSet, not `test`, so it
compiles once and is usable from the app itself, `src/test`, and `src/androidTest` alike without
duplicate-class errors (Kotlin/Java don't allow the same class on both a `test` and `androidTest`
classpath cleanly when both need it).

## Running instrumented tests via the CI script

`ci/run-instrumented-tests.sh <api-level>` wraps `connectedAndroidTest` with the setup CI needs
(waiting for the emulator to finish booting, disabling animations, installing both the app and
test APKs, granting storage permissions on API ≤ 32):

```bash
./gradlew packageDebug packageDebugAndroidTest   # build the two APKs first
./ci/run-instrumented-tests.sh 29                 # then run against a booted emulator/device
```

Useful if you're driving a local emulator the same way CI does; for routine local testing,
`./gradlew connectedAndroidTest` alone is simpler.

## Allure reporting (local unit tests only)

Local unit test results can be turned into an HTML report via Allure:

```bash
./gradlew testDebugUnitTest   # produces raw results in app/build/allure-results/
./gradlew allureReport         # requires the allure CLI on PATH; generates the HTML report
```

Report output: `app/build/reports/allure-report/`.

This is wired up via AspectJ (`aspectjweaver` javaagent injected into the test JVM), not custom
JUnit runner annotations — `allure-junit4-aspect`'s AOP hook registers the `AllureJunit4`
`RunNotifier` listener automatically the moment a `RunNotifier` is constructed, regardless of
which JUnit runner created it. Scoped to local unit tests only; instrumented (on-device) Allure
reporting isn't wired up (it would need a custom `AndroidJUnitRunner` plus pulling result files off
the device).

## Static analysis

```bash
./gradlew lint
```

HTML report: `app/build/reports/lint-results-debug.html`. As of the [baseline](BASELINE.md), this
runs clean (0 errors) with pre-existing warnings (mostly outdated-dependency notices) — a genuine
build-config bug that made `lint` fail outright (a missing task dependency on the `copyAndroidJar`
task) was found and fixed as part of establishing that baseline.

## CI

`.github/workflows/ci.yml`, on every push/PR to `main`:

- **`unit-tests`** — `./gradlew testDebugUnitTest`; results uploaded as the `unit-test-results`
  artifact (`app/build/reports/tests/`)
- **`instrumented-tests`** — builds `packageDebug packageDebugAndroidTest`, boots an API 29
  x86_64 emulator (`pixel_6` profile, 2048M RAM, 512M heap, animations disabled), runs
  `ci/run-instrumented-tests.sh 29`; results uploaded as the `instrumented-test-results` artifact
  (`app/build/reports/androidTests/connected/`)

There is currently no CI job running `lint` or uploading a built APK as an artifact — see the
[Roadmap](../README.md#roadmap) (CI/CD milestone) for planned additions there.
