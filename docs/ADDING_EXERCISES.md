# Adding a Practice Exercise

This walks through adding a new exercise to today's catalog (see
[`PRACTICE_ENGINE.md`](PRACTICE_ENGINE.md) for how the model works and its current limitations —
this is the process for the *existing* one-exact-output-string model, not the richer model planned
for later milestones).

## 1. Verify your solution independently first

Write and check your reference solution *outside* this repo first — plain `javac`/`java`, an
online compiler, whatever you trust. Don't hand-type an expected output and assume it's right;
every existing exercise's `expectedOutput` was verified against real execution before being
committed (see the doc comment at the top of `InterviewExercises.kt`).

## 2. Add the exercise

Open `app/src/testShared/java/com/javaide/mobile/compiler/InterviewExercises.kt` and add a new
`val` following the existing pattern:

```kotlin
val YOUR_EXERCISE_NAME = InterviewExercise(
    className = "YourClassName",
    source = """
        public class YourClassName {
            public static void main(String[] args) {
                // ...
                System.out.println("expected output here");
            }
        }
    """.trimIndent(),
    expectedOutput = "expected output here"
)
```

Rules that matter:

- `className` must exactly match the `public class` name inside `source` — it's used verbatim as
  the filename (`${className}.java`) when compiling.
- `source` must be a **complete, compilable Java program** with a
  `public static void main(String[] args)` — this is what gets shown in the editor, not a stub.
- `expectedOutput` is compared with `.trim()` against everything written to stdout **and**
  stderr combined during the run — if your program prints to `System.err` too, that's included.
- Keep the class name unique across the whole catalog — `PracticeCategories.find(className)`
  looks it up by exact match, and a collision would make two exercises indistinguishable.
- Prefer only the standard library unless a third-party jar is already wired in via `LibJars` —
  the exercise runs through the same on-device ECJ/D8 pipeline as everything else, so anything
  it imports has to be on that classpath.

Add your new `val` to the `ALL` list at the bottom of the file.

## 3. Put it in a category

Open `app/src/main/java/com/javaide/mobile/compiler/PracticeCategories.kt` and add
`InterviewExercises.YOUR_EXERCISE_NAME` to an existing `Category(...)`, or add a new
`Category("Your Category Name", listOf(...))` to `PracticeCategories.ALL` if it doesn't fit an
existing one. This list is what the in-app Practice screen reads directly — no other wiring is
needed for it to show up.

## 4. Add test coverage

Every exercise needs both:

**JVM compile+dex test** — `app/src/test/java/com/javaide/mobile/compiler/InterviewExercisesCompileDexTest.kt`:

```kotlin
@Test fun yourExerciseName() = assertCompilesAndDexes(InterviewExercises.YOUR_EXERCISE_NAME)
```

**Instrumented full-run test** — `app/src/androidTest/java/com/javaide/mobile/compiler/InterviewExercisesRunTest.kt`:

```kotlin
@Test fun yourExerciseName() = assertRunsWithExpectedOutput(InterviewExercises.YOUR_EXERCISE_NAME)
```

Both files already loop the same helper pattern across every exercise — just add one line to each,
matching the naming convention of the surrounding tests (camelCase, derived from the exercise
name).

## 5. Run the checks

```bash
# Fast: compiles + dexes your new exercise (and everything else) on the JVM, no device needed
./gradlew testDebugUnitTest --tests "com.javaide.mobile.compiler.InterviewExercisesCompileDexTest.yourExerciseName"

# Full: actually executes it and checks the output, needs a device/emulator
./gradlew connectedAndroidTest
```

If the compile+dex test passes but you can't run the instrumented test locally (no device
available), CI's `instrumented-tests` job will run it — but it's much faster to catch an output
mismatch yourself first, since a typo'd `expectedOutput` only surfaces there.

## 6. Manually verify in the app (recommended)

Build and install the debug APK (`./gradlew installDebug` or via Android Studio), open **Practice**,
find your exercise under its category, and tap **Run & Check** to confirm it shows "Passed" with
the editor's default (unedited) content.

## Checklist

- [ ] Solution verified against real Java execution before committing `expectedOutput`
- [ ] `className` matches the `public class` name in `source` and is unique in the catalog
- [ ] Added to `InterviewExercises.ALL`
- [ ] Placed in a category in `PracticeCategories.ALL`
- [ ] Test added to `InterviewExercisesCompileDexTest`
- [ ] Test added to `InterviewExercisesRunTest`
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew connectedAndroidTest` passes (or will be verified by CI)
