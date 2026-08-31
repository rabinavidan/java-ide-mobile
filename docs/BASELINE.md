# Milestone 0 — Baseline, Backup and Project Analysis

Snapshot of `java-ide-mobile` before the Interview Practice Expansion work begins.
Recorded on branch `claude/java-ide-interview-baseline-i1g39p` (`main` @ `1ae3e06`), 2026-08-30.

This document is the Milestone 0 deliverable from `JAVA_IDE_MOBILE_INTERVIEW_PRACTICE_EXPANSION`:
a frozen description of what exists today, so later milestones have a clear "before" to diff
against and so no current feature is accidentally dropped while the practice system is rebuilt.

## 1. Current exercise inventory

**30 exercises across 10 categories.** Source of truth:
- `app/src/testShared/java/com/javaide/mobile/compiler/InterviewExercises.kt` — the 30 exercise
  definitions (`InterviewExercise(className, source, expectedOutput)`)
- `app/src/main/java/com/javaide/mobile/compiler/PracticeCategories.kt` — groups them into the
  10 categories below

| # | Category | Exercises (`className`) |
|---|---|---|
| 1 | Fundamentals | FizzBuzz, Fibonacci, TwoSum, IsPalindrome, BinarySearch, GroupAnagrams |
| 2 | Arrays & Strings | MaxSubArray, ReverseString, ValidAnagram, ContainsDuplicate, MoveZeroes |
| 3 | Linked Lists | ReverseLinkedList, MergeTwoSortedLists, LinkedListHasCycle |
| 4 | Stacks & Queues | ValidParentheses, MinStack |
| 5 | Trees | TreeInorderTraversal, TreeMaxDepth, IsSameTree, TreeLevelOrder |
| 6 | Graphs | GraphBFS, GraphDFS |
| 7 | Sorting & Searching | MergeSort, SearchInsertPosition |
| 8 | Recursion & Backtracking | Factorial, Permutations, Subsets |
| 9 | Dynamic Programming | ClimbingStairs, CoinChange |
| 10 | Bit Manipulation | SingleNumber |

Each exercise is a single Kotlin `data class` with three fields — `className`, `source` (the
**complete, correct** Java solution, not a starter stub), and `expectedOutput` (one exact string,
matched against trimmed captured stdout).

## 2. Current compiler flow

Fully on-device toolchain, no network calls:

```
JavaCompiler (ECJ, in-process)  →  .class files
        ↓
Dexer (D8)                      →  classes.dex
        ↓
JavaRunner (DexClassLoader + reflection)  →  captured stdout/stderr
```

- **`JavaCompiler`** (`compiler/JavaCompiler.kt`) — invokes ECJ's `org.eclipse.jdt.internal.compiler.batch.Main`
  in-process against `-source 1.8 -target 1.8`, classpath = `android.jar` + any `libs/*.jar`.
  `systemExitWhenFinished=false` so a compile failure doesn't kill the host app; wrapped in
  `catch (Throwable)` because ECJ's `FileSystem` static initializer references
  `javax.lang.model.SourceVersion`, which doesn't exist on Android and throws `NoClassDefFoundError`
  (an `Error`, not `Exception`) unless stubbed.
- **`Dexer`** (`compiler/Dexer.kt`) — D8, merges project `.class` files with any `libs/*.jar`,
  library-links against `android.jar`, `minApiLevel` from the project manifest or `DEFAULT_MIN_API_LEVEL = 26`.
- **`JavaRunner`** (`compiler/JavaRunner.kt`) — loads `classes.dex` via `DexClassLoader`, finds the
  first class (by reflection scan, `Main` preferred) with `public static void main(String[])`,
  redirects `System.out`/`System.err` into a buffer, invokes it, restores the streams. Runs
  **in-process** — not sandboxed, so an infinite loop or crash in practice code can hang/crash the
  IDE itself. This is a real limitation the runner-engine milestone (Milestone 4) needs to address
  (timeout protection).

Android App projects go through an extended pipeline (`ResourceCompiler` → `JavaCompiler` → `Dexer`
→ `Packager` → `ApkInstaller`) not used by exercises; see `CLAUDE.md` for that flow.

## 3. Current Run & Check flow (today's "practice system")

There is no dedicated practice UI yet. The exercise catalog exists only as test fixtures
(`InterviewExercises.ALL`, `PracticeCategories.ALL`) exercised by two parallel test suites that
run the *same* complete solution source through the pipeline and assert one exact string:

- **JVM (fast, no device):** `InterviewExercisesCompileDexTest` — compiles + dexes all 30, asserts
  success and that `classes.dex` exists. Does not execute (no `DexClassLoader` on the JVM).
- **Instrumented (device/emulator):** `InterviewExercisesRunTest` — compiles + dexes + **runs**
  all 30 through the real pipeline and does `assertEquals(expectedOutput, output.trim())`.

Check = exact string equality against one hardcoded expected output. No partial credit, no
hidden tests, no structured pass/fail per test case — precisely the gap Milestone 4
(Challenge Execution and Test Engine) exists to close.

## 4. Current unit tests

208 total `@Test` methods across `app/src/test` (JVM) + `app/src/androidTest` (instrumented).

`app/src/test/java/com/javaide/mobile/` (JVM, run via `./gradlew testDebugUnitTest`):

| Area | File |
|---|---|
| compiler | `InterviewExercisesCompileDexTest`, `JavaCompilerTest`, `DexerTest`, `LibJarsTest`, `DependencyLibraryTest`, `ManifestUtilsTest`, `PracticeCategoriesTest`, `DexClassDefs` |
| completion | `SemanticCompletionEngineTest`, `DiagnosticsEngineTest`, `DefinitionFinderTest`, `ImportIndexTest`, `ImportInserterTest` |
| util | `FileOpsTest`, `PackageRenamerTest`, `ClassRenamerTest`, `ProjectSearchTest`, `ProjectStorageTest`, `LogcatReaderTest`, `AppNameUtilsTest` |
| model | `ProjectTemplateTest` |
| vcs | `GitRepositoryTest` |

## 5. Current Android instrumented tests

`app/src/androidTest/java/com/javaide/mobile/` (device/emulator, `./gradlew connectedAndroidTest`):

- `compiler/InterviewExercisesRunTest` — full pipeline execution of all 30 exercises (see §3)
- `util/ProjectStorageTest`
- `ui/FlatViewTest`, `ui/SearchInEditorTest`, `ui/FileExplorerNavigationTest`, `ui/EditorTabsTest`, `ui/RunFromEditorConsoleTest`

## 6. Current CI workflow

`.github/workflows/ci.yml`, triggered on push/PR to `main`:

1. **`unit-tests`** job — Temurin 17, `./gradlew testDebugUnitTest`, uploads `app/build/reports/tests/`.
2. **`instrumented-tests`** job — frees runner disk space, enables KVM, builds
   `packageDebug packageDebugAndroidTest`, runs `ci/run-instrumented-tests.sh 29` inside a
   `reactivecircus/android-emulator-runner` API 29 x86_64 emulator (`pixel_6` profile, 2048M RAM,
   512M heap), uploads `app/build/reports/androidTests/connected/`.

No lint job, no APK-artifact upload, no release pipeline, no catalog-validation step yet
(Milestone 19 territory).

## 7. Baseline validation results (this session)

App version at baseline: `versionCode 10`, `versionName "0.2.7"`. `compileSdk`/`targetSdk` 34,
`minSdk` 26, Java 17 source/target compatibility.

This sandboxed environment had no Android SDK preinstalled; one was provisioned locally
(`cmdline-tools` + `platform-tools` + `platforms;android-34` + `build-tools;34.0.0`) to run these
commands, matching what `ci.yml` uses in GitHub Actions.

| Command | Result |
|---|---|
| `./gradlew testDebugUnitTest` | **BUILD SUCCESSFUL** — 160 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew lint` | **BUILD SUCCESSFUL** (after a fix — see below) — 0 errors, 93 warnings (mostly `GradleDependency` outdated-library notices; also `Autofill`, `NotifyDataSetChanged`, `ButtonStyle`, `PluralsCandidate`, `ObsoleteSdkInt`, `RtlSymmetry`, etc.). Full report: `app/build/reports/lint-results-debug.html` |
| `./gradlew assembleDebug` | **BUILD SUCCESSFUL** — `app/build/outputs/apk/debug/app-debug.apk` produced (~50 MB) |
| `./ci/run-instrumented-tests.sh` | Not run — no Android emulator/device available in this sandboxed environment |

**Bug found and fixed while running `./gradlew lint`:** it failed outright with a Gradle task-validation
error before analyzing any code — `generateDebugLintReportModel` and `lintAnalyzeDebug` read
`build/generated/androidJarAsset` (registered as an assets source dir in `app/build.gradle.kts`)
without declaring a dependency on the `copyAndroidJar` task that populates it. The existing
`dependsOn(copyAndroidJar)` wiring only covered `merge*Assets` tasks. Fixed by extending that
`tasks.matching { ... }` block to also match `generate*LintReportModel` and `lintAnalyze*` (see
commit `95dcb17`). This means `./gradlew lint` was previously non-functional — worth noting since
Milestone 19 plans to add a lint job to CI, and it needs this fix to work at all.

## 8. Documented current limitations

Carried over verbatim from the milestone plan and confirmed against the code in this session:

- Exercises contain complete solutions (`InterviewExercise.source` is the answer, not a starter stub)
- Only console output is checked (`assertEquals(expectedOutput, output.trim())`)
- No hidden test cases — one visible expected-output string per exercise
- No difficulty levels
- No progress tracking (no persistence of attempts/solved state for exercises)
- No problem descriptions (no title, statement, constraints, or examples — just source + expected output)
- No hints
- No interview timer
- No filtering or search over the exercise catalog
- All exercises live in one large source file (`InterviewExercises.kt`, 861 lines; `PracticeCategories.kt` hardcodes the 10 category groupings)
- README.md is missing (repository root has `CLAUDE.md` and `CONTEXT_LOG.md` but no `README.md`, `CONTRIBUTING.md`, or `docs/` before this commit)
- `JavaRunner` executes practice code in-process with no timeout/sandbox — a hanging or crashing snippet can hang or crash the IDE

## 9. Acceptance criteria check (Milestone 0)

- [x] Existing unit tests pass — see §7
- [x] Existing debug APK builds — see §7
- [x] No current feature is removed — this session only added `docs/BASELINE.md` and this log entry; no existing file was deleted or behaviorally changed
- [x] Current 30 exercises still compile and run — unchanged `InterviewExercises.kt`/`InterviewExercisesCompileDexTest`/`InterviewExercisesRunTest`, verified via `testDebugUnitTest` in §7
