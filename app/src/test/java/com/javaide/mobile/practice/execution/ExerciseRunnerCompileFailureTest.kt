package com.javaide.mobile.practice.execution

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
import java.io.File

/**
 * Exercises [ExerciseRunner]'s compile-failure path against the *real* ECJ compiler (the same
 * one [com.javaide.mobile.compiler.JavaCompiler] uses) — this doesn't need `DexClassLoader`, so
 * unlike the rest of the engine it can run as a plain JVM test, the same way
 * `InterviewExercisesCompileDexTest` does for the legacy catalog.
 */
class ExerciseRunnerCompileFailureTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var androidJar: File

    @Before
    fun setUp() {
        val path = System.getProperty("android.jar.path")
            ?: error("android.jar.path system property not set; see app/build.gradle.kts")
        androidJar = File(path)
        check(androidJar.isFile) { "android.jar not found at $path" }
    }

    private fun fixtureExercise(className: String) = InterviewExercise(
        id = "test-$className",
        title = className,
        className = className,
        categoryId = "test-category",
        difficulty = Difficulty.EASY,
        description = "A fixture for ExerciseRunner tests.",
        constraints = emptyList(),
        examples = listOf(ExerciseExample("in", "out", "explanation")),
        starterCode = "",
        solutionCode = "",
        hints = emptyList(),
        testCases = listOf(
            ExerciseTestCase("t1", "", "5", visible = true, description = "d1"),
            ExerciseTestCase("t2", "", "6", visible = false, description = "d2")
        ),
        timeComplexity = "O(1)",
        spaceComplexity = "O(1)",
        patterns = emptySet(),
        tags = emptySet(),
        estimatedMinutes = 5
    )

    @Test
    fun `a syntax error produces a compiled=false result with no test executions`() {
        val exercise = fixtureExercise("BrokenSyntax")
        val brokenSource = """
            public class BrokenSyntax {
                public static void main(String[] args) {
                    System.out.println("missing closing brace"
                }
            // missing closing brace for the class too
        """.trimIndent()

        val result = ExerciseRunner.run(exercise, brokenSource, tempFolder.newFolder("work1"), androidJar)

        assertFalse(result.compiled)
        assertFalse(result.passed)
        assertEquals(0, result.passedTests)
        assertEquals(2, result.totalTests)
        assertTrue(result.compilationErrors.isNotEmpty())
        assertTrue(result.testResults.isEmpty())
    }

    @Test
    fun `a class name mismatch also fails to compile`() {
        val exercise = fixtureExercise("ExpectedName")
        val mismatchedSource = """
            public class ActualName {
                public static void main(String[] args) {
                    System.out.println("hello");
                }
            }
        """.trimIndent()

        val result = ExerciseRunner.run(exercise, mismatchedSource, tempFolder.newFolder("work2"), androidJar)

        assertFalse(result.compiled)
        assertEquals(0, result.passedTests)
        assertEquals(2, result.totalTests)
    }
}
