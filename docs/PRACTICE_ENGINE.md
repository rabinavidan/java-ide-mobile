# Practice Engine

How the interview-practice catalog and Run & Check flow work today, and where they're headed.
For the exercise-authoring workflow, see [`ADDING_EXERCISES.md`](ADDING_EXERCISES.md). For the
full baseline snapshot this document formalizes, see [`BASELINE.md`](BASELINE.md).

## The model, today

```kotlin
data class InterviewExercise(val className: String, val source: String, val expectedOutput: String)
```

Defined in `app/src/testShared/java/com/javaide/mobile/compiler/InterviewExercises.kt`. Note the
package: `com.javaide.mobile.compiler`, and the source set: `testShared` — added to the `main`
sourceSet (not `test`) specifically so both the app itself and the local/instrumented test suites
can reference `InterviewExercises` without duplicate-class errors. In other words, **the practice
catalog ships inside the app APK** — it isn't test-only scaffolding.

Each exercise has exactly three fields:

- `className` — must match the `public class` name in `source` (it's used as the filename when
  compiling: `${className}.java`)
- `source` — a **complete, correct** Java program (not a starter stub — the practice UI shows this
  directly, pre-filled, as something to study and tweak)
- `expectedOutput` — one string, matched exactly (after `.trim()`) against everything the program
  writes to stdout/stderr during its run

`PracticeCategories.kt` groups the 30 exercises in `InterviewExercises.ALL` into 10 named
categories (`Category(title, exercises)`), and provides:

- `PracticeCategories.ALL` — the category list, read live by `PracticeActivity`'s adapter (no
  hardcoded count/list anywhere in the UI)
- `PracticeCategories.find(className)` — catalog lookup used by `PracticeDetailActivity`
- `PracticeCategories.displayTitle(exercise)` — turns `"GraphBFS"` into `"Graph BFS"` for display,
  via a camelCase-boundary regex

## The Run & Check flow, today

`PracticeDetailActivity.execute()` (see `app/src/main/java/com/javaide/mobile/ui/PracticeDetailActivity.kt`):

1. Take whatever's currently in the editor (starts as `exercise.source`, but the user can edit it)
2. Write it to `cacheDir/practice/<className>/src/main/java/<className>.java`
3. `JavaCompiler.compile(...)` — on failure, show the compile log and stop
4. `Dexer.dex(...)` — on failure, show the dex log and stop
5. `JavaRunner.run(...)` — capture stdout/stderr
6. Pass/fail = `runResult.success && runResult.output.trim() == exercise.expectedOutput`
7. Show a green ("Passed") or red ("Failed") status banner plus the raw captured output

This is the *same* on-device compile/dex/run pipeline used for a normal Java Console project — see
[`ARCHITECTURE.md`](ARCHITECTURE.md) — just pointed at a temp directory under `cacheDir` instead
of a saved project.

The two test suites (`InterviewExercisesCompileDexTest` for JVM compile+dex-only,
`InterviewExercisesRunTest` for the full on-device compile+dex+run+compare) exercise this same
path against every exercise's *unmodified* `source`, so they double as a guard that no exercise's
reference solution has silently stopped compiling or producing its expected output.

## Current constraints

- **One exact-match string, not structured tests.** There's no notion of multiple test cases,
  hidden tests, or partial credit — `output.trim() == expectedOutput` is the entire evaluation.
- **The "starter code" is the finished solution.** A user practices by reading and modifying a
  correct answer, not by writing one from scratch against a spec.
- **No problem statement.** No title, description, constraints, or examples beyond the
  category-derived display title (`PracticeCategories.displayTitle`) and the code itself.
- **No difficulty, hints, timer, search, or filtering.**
- **No progress persistence.** Pass/fail is shown once, in-memory, for that Run & Check click;
  nothing is saved about whether you've seen or solved an exercise before.
- **Unsandboxed execution.** Same caveat as the general runner: an edited solution that hangs or
  crashes runs in-process and can hang/crash the app.

## Where this is headed

The interview-practice expansion plan (see the roadmap link in the root `README.md`) replaces this
model in stages:

1. **Model V2** — `Difficulty`, `PracticeMode`, a richer `InterviewExercise` with separate
   `starterCode`/`solutionCode`, `ExerciseExample`s, `ExerciseTestCase`s (visible + hidden),
   hints, complexity metadata, and an `ExerciseValidator`
2. **Modular catalog** — today's single 861-line `InterviewExercises.kt` split into per-topic
   files under `practice/catalog/`, behind an `ExerciseCatalog` interface with
   `findById`/`findByCategory`/`findByDifficulty`/`search`
3. **A real execution engine** — `ExerciseRunner`/`TestCaseRunner`/`ExerciseResultEvaluator`
   running multiple structured test cases per exercise, with per-test timeout protection,
   replacing the single `output.trim() == expectedOutput` check
4. **Learn / Practice / Interview modes** — the current Run & Check screen becomes Practice Mode's
   editor pane; Learn Mode adds explanations and a visible reference solution; Interview Mode adds
   a timer and hides hints/solutions/hidden-test expectations

None of stage 1–4 is implemented yet as of this document. This file describes the system as it
exists *today* so a contributor can find their way around it before any of that lands; update it
as each stage ships.
