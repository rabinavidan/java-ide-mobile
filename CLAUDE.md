# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug APK
./gradlew assembleDebug

# Local unit tests (JVM, no device needed)
./gradlew testDebugUnitTest

# Single local test
./gradlew testDebugUnitTest --tests "com.javaide.mobile.compiler.InterviewExercisesCompileDexTest.fizzBuzz"

# Generate Allure HTML report (requires allure CLI on PATH)
./gradlew allureReport

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

Allure results land in `app/build/allure-results/`; HTML report in `app/build/reports/allure-report/`.

## Architecture

Single `:app` module. The app is a mobile Java IDE that compiles and runs Java code entirely on-device using an in-process toolchain.

### Compile → Run Pipeline (Java Console projects)

`JavaCompiler` (ECJ) → `.class` files → `Dexer` (D8) → `classes.dex` → `JavaRunner` (DexClassLoader + reflection to call `main()`)

### Compile → Build → Install Pipeline (Android App projects)

`ResourceCompiler` (ARSCLib) generates `R.java` + binary resources → `JavaCompiler` → `Dexer` → `Packager` (apksig) → `ApkInstaller`

The pipeline is orchestrated by `FileExplorerActivity.runProject()` / `runBuild()`. Each step captures logs; failures stop the pipeline and display results in `BuildOutputActivity`.

### UI Flow

`MainActivity` (project list) → `FileExplorerActivity` (file browser, triggers build/run) → `EditorActivity` (Sora Editor, multi-tab) → `BuildOutputActivity` (logs/results)

### Key Packages

| Package | Purpose |
|---|---|
| `com.javaide.mobile.compiler` | Full toolchain: `JavaCompiler`, `Dexer`, `JavaRunner`, `ResourceCompiler`, `Packager`, `ApkInstaller`, `AndroidJarProvider`, `DebugSigningKey`, `ManifestUtils`, `LibJars` |
| `com.javaide.mobile.ui` | Activities and RecyclerView adapters |
| `com.javaide.mobile.model` | `ProjectTemplate` — scaffolds Android App / Java Console projects |
| `com.javaide.mobile.util` | `ProjectStorage`, `FileOps`, `PackageRenamer`, `ClassRenamer`, `LogcatReader`, `ProjectSearch` |
| `com.javaide.mobile.completion` | `SemanticCompletionEngine`, `DiagnosticsEngine`, `ImportIndex`, `ImportInserter`, `DefinitionFinder` |
| `com.javaide.mobile.data` | Room database: `AppDatabase`, `AppDao`, `EditorState`, `EventEntry`, `Logger` |
| `com.javaide.mobile.vcs` | `GitRepository`, `GitCommitSigner`, `PgpKeyManager` |

### Test Structure

- `src/test/` — Local JVM tests (`InterviewExercisesCompileDexTest`: compile + dex 30 coding exercises)
- `src/androidTest/` — Instrumented tests (`InterviewExercisesRunTest`: execute compiled dex on device)
- `src/testShared/` — Shared fixtures compiled into `main` sourceSet (avoids duplicate class collision); contains `InterviewExercises` with 30 exercise sources + expected outputs

Local tests need `android.jar` available; the test task sets `android.jar.path` as a system property via `app/build.gradle.kts`.

## Key Conventions

**Project storage layout:** `context.filesDir/projects/<ProjectName>/src/main/java|res/`, build artifacts in `build/classes/`, `build/dex/`, `build/generated/r/`, `build/outputs/`.

**Third-party JARs:** Auto-discovered from `libs/*.jar` in the project root — no config needed. `LibJars` passes them to both ECJ compile classpath and D8 merge.

**Async pattern:** All compile/dex/run work runs on `Dispatchers.IO` via `lifecycleScope.launch`; UI updates via `withContext(Dispatchers.Main.immediate)`.

**Editor debouncing:** Auto-save and diagnostics both trigger 1.5s after last keystroke. `EditorActivity` reuses a single Sora `CodeEditor` instance across tabs (undo history resets on tab switch; auto-save protects file content).

**Cursor persistence:** `EditorState` (Room) saves cursor line/column per file path; restored on next editor open.

## Non-Obvious Details

- `android.jar` is bundled in assets and extracted on first run by `AndroidJarProvider`; the Gradle `copyAndroidJar` task populates it from `bootClasspath` at build time — do not commit the binary manually.
- `JavaRunner` loads dex in-process via `DexClassLoader` + reflection; stdout/stderr are captured by redirecting `System.out`/`System.err`.
- APK signing always uses the same debug key (`DebugSigningKey`), generated once and reused.
- `DiagnosticsEngine` re-invokes ECJ on every debounced keystroke using a placeholder token for incomplete expressions.
- `src/testShared/` is added to the `main` sourceSet (not `test`) to share fixtures between local and instrumented tests without duplicate class errors.
- Allure reporting is aspect-based (AspectJ javaagent injected in test JVM args) — no test runner annotations needed.

## Key Dependencies

- **ECJ 3.46.0** — Java compiler (runs in-process)
- **R8/D8 9.1.31** — DEX compiler
- **Sora Editor 0.23.6** — Code editor with Java syntax highlighting
- **ARSCLib 1.4.0** — Android resource compilation
- **apksig 9.3.1** — APK signing
- **Room** — Editor state + event log persistence
- **JGit + BouncyCastle** — Git operations and PGP-signed commits
- **Allure 2.29.0 + aspectjweaver** — Test reporting

## SDK and Java Config

- `compileSdk` / `targetSdk`: 34, `minSdk`: 26
- Source/target compatibility: Java 17
- View Binding enabled
