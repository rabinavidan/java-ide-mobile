# Context Log

Running log of all development activity in this repository.

---

## 2026-08-22

### Session Start
- Branch: `claude/android-java-app-e60ews`
- User: rabinabdian

### Activity
- `CLAUDE.md` — Improved with expanded package table, corrected Android App pipeline order, added conventions and non-obvious implementation details
- `CONTEXT_LOG.md` — Created this file to track ongoing development activity
- `Logger.kt` — Added rotating on-device file logging (`filesDir/logs/activity.log`, 512 KB max, 1 backup); all existing Room events now also written to file
- `EditorActivity.kt` — Added Logger calls for: file opened, file saved (auto-save), tab closed

---

## 2026-08-30

### Session Start
- Branch: `claude/java-ide-interview-baseline-i1g39p`
- Task: Milestone 0 (Baseline, Backup and Project Analysis) of the Interview Practice Expansion plan

### Activity
- `docs/BASELINE.md` — Created: records the current exercise inventory (30 exercises, 10
  categories, confirmed against `InterviewExercises.kt`/`PracticeCategories.kt`), the
  compile/dex/run pipeline, the current Run & Check flow (exact-string match, no hidden tests),
  the existing unit/instrumented test suites (208 `@Test` methods total), the CI workflow, and
  the documented current limitations
- `app/build.gradle.kts` — Fixed `./gradlew lint`, which previously failed outright with a Gradle
  task-validation error: `generateDebugLintReportModel`/`lintAnalyzeDebug` read
  `build/generated/androidJarAsset` without a declared dependency on the `copyAndroidJar` task
  that populates it. Extended the existing `merge*Assets` `dependsOn` wiring to also cover the
  lint tasks
- `.gitignore` — Added `.kotlin/` (local Gradle build cache directory)
- Ran baseline validation in this sandboxed environment (no Android SDK preinstalled here;
  provisioned `cmdline-tools` + `platform-tools` + `platforms;android-34` + `build-tools;34.0.0`
  locally to match CI): `./gradlew testDebugUnitTest` (160 tests, all passing),
  `./gradlew lint` (0 errors, 93 warnings, after the fix above),
  `./gradlew assembleDebug` (debug APK builds successfully). Instrumented tests were not run —
  no emulator/device available here

---

## 2026-08-31

### Session Start
- Branch: `claude/java-ide-interview-baseline-i1g39p`
- Task: Milestone 1 (Repository Documentation), then a direct coverage audit, then materializing
  the full milestone plan into the repo with a new Milestone 23

### Activity
- `README.md`, `CONTRIBUTING.md`, `docs/ARCHITECTURE.md`, `docs/PRACTICE_ENGINE.md`,
  `docs/ADDING_EXERCISES.md`, `docs/TESTING.md` — created (Milestone 1). Every command referenced
  was actually run and verified, not assumed. Also surfaced that the practice catalog already has
  a real in-app UI (`PracticeActivity` → `PracticeDetailActivity`), which Milestone 0's baseline
  had only described as test fixtures — corrected in `docs/PRACTICE_ENGINE.md`. Merged via
  [PR #40](https://github.com/rabinavidan/java-ide-mobile/pull/40)
- Ran a static class-by-class test-coverage audit on request (no JaCoCo configured, so this was a
  manual comparison of `app/src/main/java` against `app/src/test`/`app/src/androidTest`): ~24/55
  main classes (~44%) have any dedicated test. Biggest gaps: the entire `data` (Room) package,
  `Packager`/`ResourceCompiler`/`ApkInstaller`/`DebugSigningKey`, `GitCommitSigner`/`PgpKeyManager`,
  `JavaRunner` (unit-level), and most of the UI layer including the Practice screens
- `docs/ROADMAP.md` — created: the full 22-milestone Interview Practice Expansion plan (previously
  only living in task/conversation context, not the repo), plus a new **Milestone 23 — Test
  Coverage Hardening (JaCoCo + closing the gaps)** appended at the end, addressing the audit
  findings above
- `README.md` — Roadmap section now points at `docs/ROADMAP.md` as the canonical plan
- Milestone 2 (Practice domain model V2): added `com.javaide.mobile.practice.model`
  (`Difficulty`, `PracticeMode`, `ExerciseExample`, `ExerciseTestCase`, the V2
  `InterviewExercise`), `com.javaide.mobile.practice.validation.ExerciseValidator` (structural
  checks: unique id/className across a catalog, valid Java class name, non-empty test
  cases/examples, non-blank description/starter/solution code, positive estimatedMinutes — the
  two compile-time rules from the plan are left to the existing compiler-integration test suite,
  not duplicated here), and `com.javaide.mobile.practice.migration.LegacyExerciseMigration`
  (converts all 30 existing exercises into the V2 model with clearly-labeled placeholder metadata
  for fields the legacy model has no data for). Purely additive — the existing
  `com.javaide.mobile.compiler.InterviewExercise`/`InterviewExercises`/`PracticeCategories` and
  the live Practice UI are untouched. 19 new unit tests; full suite now 179 tests, all passing

---

## 2026-08-31

### Session Start
- Branch: `claude/java-ide-interview-baseline-i1g39p` (restarted from `main` after the Milestone 0
  PR merged — see `#39`)
- Task: Milestone 1 (Repository Documentation) of the Interview Practice Expansion plan

### Activity
- `README.md` — Created: project overview, product purpose, main features, supported project
  types, Java compilation flow, practice system, existing exercise categories, local build
  instructions, emulator requirements, test commands, CI workflow, project architecture summary,
  roadmap, contribution pointer, known limitations
- `CONTRIBUTING.md` — Created: dev setup, pre-PR checks, where to look before touching the
  compiler/practice code, commit/PR conventions, issue-reporting guidance
- `docs/ARCHITECTURE.md` — Created: UI flow diagram, compile→run and compile→build→install
  pipeline diagrams, current vs. target practice-flow diagrams (target per the expansion plan's
  challenge→catalog→editor→compiler→runner→evaluator→progress shape), package table, data layer,
  key conventions
- `docs/PRACTICE_ENGINE.md` — Created: documents the current `InterviewExercise`
  model/`PracticeCategories`/`PracticeDetailActivity.execute()` Run & Check flow in detail
  (discovered the practice system already has a real UI, not just test fixtures — Milestone 0's
  baseline undersold this), its current constraints, and the staged plan to replace it
- `docs/ADDING_EXERCISES.md` — Created: step-by-step contributor guide with a checklist
- `docs/TESTING.md` — Created: test suite breakdown, Allure reporting, static analysis, CI jobs
- Verified every command referenced in the new docs actually runs: `testDebugUnitTest` (full and
  single-test `--tests` filter), `lint`, `assembleDebug`, `packageDebug packageDebugAndroidTest`;
  confirmed `installDebug`/`allureReport`/`connectedAndroidTest` tasks exist via `./gradlew tasks --all`

---

## 2026-08-31 (continued) — Milestone 3

### Session Start
- Branch: `claude/java-ide-interview-baseline-i1g39p` (restarted from `main` after the Milestone 2
  / `docs/ROADMAP.md` PR merged — see `#41`)
- Task: Milestone 3 (Modular Exercise Catalog) of the Interview Practice Expansion plan

### Activity
- `LegacyExerciseMigration.categoryIdFor(title)` — exposed the previously-private slugify logic
  so the new catalog files below don't hardcode/duplicate category-id slugs
- `practice/catalog/ExerciseCatalog.kt` — the interface: `getAll`/`findById`/`findByCategory`/
  `findByDifficulty`/`findByPattern`/`search`
- `practice/catalog/InMemoryExerciseCatalog.kt` — a reusable `ExerciseCatalog` implementation over
  any fixed list (used both by the app-wide registry and by tests with hand-built fixtures)
- `practice/catalog/{Fundamentals,Arrays,LinkedList,StackQueue,Tree,Graph,SortingSearching,
  RecursionBacktracking,DynamicProgramming,BitManipulation}Exercises.kt` — the 30 exercises split
  into 10 topic files (one per today's actual category), each declaring its own legacy-exercise
  membership and migrating them via `LegacyExerciseMigration`. Doesn't create the full 16-file
  target structure from the plan — several of those names (HashMapExercises, TwoPointersExercises,
  AutomationExercises, ...) correspond to categories that don't have any content yet (Milestones
  6-11); creating empty stub files for them now would be scope creep, so those get created when
  their content does
- `practice/catalog/ExerciseCatalogRegistry.kt` — the app-wide `ExerciseCatalog` singleton,
  aggregating all 10 topic files. Not yet wired into the live Practice UI (needs the execution
  engine and starter/solution separation from Milestones 4-5 first) — `PracticeCategories`/
  `InterviewExercises` remain the UI's data source, unchanged
- 13 new unit tests (`ExerciseCatalogRegistryTest` against the real 30-exercise catalog,
  `InMemoryExerciseCatalogTest` against hand-built fixtures for difficulty/pattern/search
  filtering that the migrated placeholder content doesn't yet exercise); full suite now 192
  tests, all passing

---

## 2026-08-31 (continued)

### Activity — Milestone 4 (Challenge Execution and Test Engine)
- `practice/execution/{TestCaseResult,ExerciseRunResult,RawExecution}.kt` — the structured
  run-result models from the plan (stdout/stderr captured separately in `RawExecution`, folded
  into a single `actual` for comparison purposes by the evaluator)
- `practice/execution/ExerciseResultEvaluator.kt` — pure logic, no Android dependency, so fully
  unit-testable on the JVM: safe output normalization (line-ending unification, trailing
  whitespace per line, trailing blank-line collapse — deliberately *not* collapsing internal
  whitespace or hiding genuinely extra output), per-test-case pass/fail evaluation (a timeout or
  uncaught exception always fails, regardless of partial stdout), aggregate stats, and an
  Interview Mode redaction helper that hides only a hidden test's `expected` value (never
  `actual`/`passed` — those describe the user's own submission)
- `practice/execution/TestCaseRunner.kt` — runs one compiled program invocation per test case via
  `DexClassLoader`, feeding `testCase.input` through stdin, with a per-test timeout via a bounded
  executor. Deliberately duplicates (rather than shares) `JavaRunner`'s small main-method-finding
  logic, keeping the existing shipped Run flow for normal Java Console projects completely
  unaffected by this milestone. Documented explicitly: the timeout is best-effort — a genuinely
  hung thread can't be forcibly killed in-process, so `run()` stops *waiting* at the timeout but
  can't guarantee the runaway thread actually stops (same non-sandboxed caveat `JavaRunner`
  already carries, not a new regression)
- `practice/execution/ExerciseRunner.kt` — orchestrates compile → dex → one `TestCaseRunner` call
  per test case → `ExerciseResultEvaluator`; one test case failing never stops the rest. The
  compile/dex-failure early-return paths never touch `DexClassLoader`, so they're unit-testable
  on the JVM the same way `JavaCompiler`/`Dexer` already are
- 17 new JVM unit tests (`ExerciseResultEvaluatorTest`: normalization edge cases, correct/incorrect
  solution, extra console output, empty output, runtime exception, timeout representation,
  aggregation, hidden-test redaction; `ExerciseRunnerCompileFailureTest`: real ECJ compile failures
  through the full `ExerciseRunner.run()` path) — full JVM suite now 209 tests, all passing
- `ExerciseRunnerRunTest.kt` (instrumented, androidTest) — the scenarios that need real
  `DexClassLoader` execution and can't run on a plain JVM: correct solution (multiple test cases
  via stdin), incorrect solution failing multiple test cases without stopping early, runtime
  exception, infinite-loop timeout (asserted to return well under 30s, not hang), extra console
  output, and empty-output pass/fail. Verified to compile and package
  (`compileDebugAndroidTestKotlin`/`packageDebugAndroidTest` both succeed) — no emulator available
  in this sandboxed environment to actually execute it, so CI's instrumented job is this file's
  first real run
- Not wired into the live UI yet — same deferral as Milestone 3, waiting on starter/solution
  separation (Milestone 5) before a Run & Check click has a meaningful "starter code" to run

---
