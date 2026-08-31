package com.javaide.mobile.practice.execution

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.javaide.mobile.compiler.AndroidJarProvider
import com.javaide.mobile.practice.model.Difficulty
import com.javaide.mobile.practice.model.ExerciseExample
import com.javaide.mobile.practice.model.ExerciseTestCase
import com.javaide.mobile.practice.model.InterviewExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs [ExerciseRunner] end to end on-device — the parts that need `DexClassLoader`
 * ([TestCaseRunner]) and so can't be covered by a plain JVM test (see
 * `ExerciseRunnerCompileFailureTest` for the compile-failure path, which can). Covers every
 * scenario Milestone 4 calls out: correct solution, incorrect solution, multiple failing test
 * cases, runtime exception, infinite loop/timeout, extra console output, and empty output.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseRunnerRunTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var androidJar: File
    private var workDirCounter = 0

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        androidJar = AndroidJarProvider.get(context)
    }

    private fun newWorkDir(): File = tempFolder.newFolder("work-${workDirCounter++}")

    private fun fixtureExercise(className: String, testCases: List<ExerciseTestCase>) = InterviewExercise(
        id = "test-$className",
        title = className,
        className = className,
        categoryId = "test-category",
        difficulty = Difficulty.EASY,
        description = "A fixture for ExerciseRunner instrumented tests.",
        constraints = emptyList(),
        examples = listOf(ExerciseExample("in", "out", "explanation")),
        starterCode = "",
        solutionCode = "",
        hints = emptyList(),
        testCases = testCases,
        timeComplexity = "O(1)",
        spaceComplexity = "O(1)",
        patterns = emptySet(),
        tags = emptySet(),
        estimatedMinutes = 5
    )

    // --- correct / incorrect solution, multiple test cases ---

    private val sumTwoNumbersTestCases = listOf(
        ExerciseTestCase("t1", input = "2 3", expectedOutput = "5", visible = true, description = "small positives"),
        ExerciseTestCase("t2", input = "10 20", expectedOutput = "30", visible = true, description = "larger positives"),
        ExerciseTestCase("t3", input = "-5 5", expectedOutput = "0", visible = false, description = "cancels out")
    )

    private val correctSumSource = """
        import java.util.Scanner;
        public class SumTwoNumbers {
            public static void main(String[] args) {
                Scanner sc = new Scanner(System.in);
                int a = sc.nextInt();
                int b = sc.nextInt();
                System.out.println(a + b);
            }
        }
    """.trimIndent()

    private val incorrectSumSource = """
        import java.util.Scanner;
        public class SumTwoNumbers {
            public static void main(String[] args) {
                Scanner sc = new Scanner(System.in);
                int a = sc.nextInt();
                int b = sc.nextInt();
                System.out.println(a - b);
            }
        }
    """.trimIndent()

    @Test
    fun correctSolutionPassesAllTestCases() {
        val exercise = fixtureExercise("SumTwoNumbers", sumTwoNumbersTestCases)
        val result = ExerciseRunner.run(exercise, correctSumSource, newWorkDir(), androidJar)

        assertTrue(result.compiled)
        assertTrue(result.passed)
        assertEquals(3, result.passedTests)
        assertEquals(3, result.totalTests)
        assertTrue(result.testResults.all { it.errorMessage == null })
    }

    @Test
    fun incorrectSolutionFailsMultipleTestCasesWithoutStoppingEarly() {
        val exercise = fixtureExercise("SumTwoNumbers", sumTwoNumbersTestCases)
        val result = ExerciseRunner.run(exercise, incorrectSumSource, newWorkDir(), androidJar)

        assertTrue(result.compiled)
        assertFalse(result.passed)
        assertEquals(0, result.passedTests)
        // All three test cases must have actually run (and been reported), not just the first one.
        assertEquals(3, result.testResults.size)
        assertTrue(result.testResults.all { !it.passed })
        assertEquals(setOf("t1", "t2", "t3"), result.testResults.map { it.testCaseId }.toSet())
    }

    // --- runtime exception ---

    @Test
    fun runtimeExceptionFailsTheTestWithAnErrorMessage() {
        val exercise = fixtureExercise(
            "ThrowsException",
            listOf(ExerciseTestCase("t1", input = "", expectedOutput = "unreachable", visible = true, description = "always throws"))
        )
        val source = """
            public class ThrowsException {
                public static void main(String[] args) {
                    throw new RuntimeException("boom");
                }
            }
        """.trimIndent()

        val result = ExerciseRunner.run(exercise, source, newWorkDir(), androidJar)

        assertTrue(result.compiled)
        assertFalse(result.passed)
        assertEquals(1, result.testResults.size)
        val testResult = result.testResults.single()
        assertFalse(testResult.passed)
        assertTrue(testResult.errorMessage!!.contains("boom"))
    }

    // --- infinite loop / timeout ---

    @Test
    fun infiniteLoopTimesOutWithoutHangingTheCaller() {
        val exercise = fixtureExercise(
            "InfiniteLoop",
            listOf(ExerciseTestCase("t1", input = "", expectedOutput = "unreachable", visible = true, description = "never finishes"))
        )
        val source = """
            public class InfiniteLoop {
                public static void main(String[] args) {
                    while (true) { }
                }
            }
        """.trimIndent()

        val timeoutMs = 1000L
        val startWallClock = System.currentTimeMillis()
        val result = ExerciseRunner.run(exercise, source, newWorkDir(), androidJar, testTimeoutMs = timeoutMs)
        val wallClockElapsed = System.currentTimeMillis() - startWallClock

        assertTrue(result.compiled)
        assertFalse(result.passed)
        val testResult = result.testResults.single()
        assertFalse(testResult.passed)
        assertTrue(testResult.errorMessage!!.contains("timed out", ignoreCase = true))
        // The call must return promptly once the timeout elapses -- generous upper bound to absorb
        // compile/dex time, well under "hung forever".
        assertTrue("expected to return well before 30s, took ${wallClockElapsed}ms", wallClockElapsed < 30_000)
    }

    // --- extra console output ---

    @Test
    fun extraConsoleOutputFailsTheComparison() {
        val exercise = fixtureExercise(
            "ExtraOutput",
            listOf(ExerciseTestCase("t1", input = "", expectedOutput = "5", visible = true, description = "prints extra debug text"))
        )
        val source = """
            public class ExtraOutput {
                public static void main(String[] args) {
                    System.out.println("5");
                    System.out.println("DEBUG: unexpected extra line");
                }
            }
        """.trimIndent()

        val result = ExerciseRunner.run(exercise, source, newWorkDir(), androidJar)

        assertTrue(result.compiled)
        assertFalse(result.passed)
        val testResult = result.testResults.single()
        assertFalse(testResult.passed)
        assertTrue(testResult.actual.contains("DEBUG"))
    }

    // --- empty output: matching pass, and a mismatch fail, in the same run ---

    @Test
    fun emptyOutputPassesWhenExpectedAndFailsWhenContentWasExpected() {
        val exercise = fixtureExercise(
            "PrintsNothing",
            listOf(
                ExerciseTestCase("expects-empty", input = "", expectedOutput = "", visible = true, description = "matches empty output"),
                ExerciseTestCase("expects-content", input = "", expectedOutput = "5", visible = true, description = "would need content that never comes")
            )
        )
        val source = """
            public class PrintsNothing {
                public static void main(String[] args) {
                }
            }
        """.trimIndent()

        val result = ExerciseRunner.run(exercise, source, newWorkDir(), androidJar)

        assertTrue(result.compiled)
        assertFalse(result.passed)
        assertEquals(1, result.passedTests)
        assertEquals(2, result.totalTests)

        val emptyCase = result.testResults.first { it.testCaseId == "expects-empty" }
        val contentCase = result.testResults.first { it.testCaseId == "expects-content" }
        assertTrue(emptyCase.passed)
        assertFalse(contentCase.passed)
    }
}
