package com.javaide.mobile.practice.execution

import com.javaide.mobile.compiler.Dexer
import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.practice.model.ExerciseTestCase
import com.javaide.mobile.practice.model.InterviewExercise
import java.io.File

/**
 * Orchestrates the full challenge-execution flow for one [InterviewExercise] (Milestone 4):
 * compile once, dex once, then run [TestCaseRunner] once per test case, evaluating each via
 * [ExerciseResultEvaluator]. One test case failing (a mismatch, a runtime exception, a timeout)
 * never stops the remaining ones — every test case in [InterviewExercise.testCases] always gets
 * a [TestCaseResult].
 *
 * The compile- and dex-failure paths never touch [TestCaseRunner] (no `DexClassLoader`
 * involved), so — unlike the rest of this class — they're exercisable in a plain JVM unit test
 * the same way [com.javaide.mobile.compiler.JavaCompiler]/[Dexer] already are.
 */
object ExerciseRunner {

    const val DEFAULT_TEST_TIMEOUT_MS = 5_000L

    fun run(
        exercise: InterviewExercise,
        source: String,
        workDir: File,
        androidJar: File,
        testTimeoutMs: Long = DEFAULT_TEST_TIMEOUT_MS
    ): ExerciseRunResult {
        val startNanos = System.nanoTime()
        workDir.deleteRecursively()
        val javaDir = File(workDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "${exercise.className}.java").writeText(source)

        val classesDir = File(workDir, "classes")
        val compileResult = JavaCompiler.compile(workDir, classesDir, androidJar)
        if (!compileResult.success) {
            return failedBeforeRunning(exercise, listOf(compileResult.log), elapsedMs(startNanos))
        }

        val dexDir = File(workDir, "dex")
        val dexResult = Dexer.dex(classesDir, dexDir, androidJar)
        if (!dexResult.success) {
            return failedBeforeRunning(exercise, listOf(dexResult.log), elapsedMs(startNanos))
        }

        val dexFile = File(dexDir, "classes.dex")
        val testResults = exercise.testCases.map { testCase ->
            runOneTestCase(testCase, classesDir, dexFile, workDir, testTimeoutMs)
        }

        return ExerciseResultEvaluator.aggregate(
            exerciseId = exercise.id,
            compiled = true,
            compilationErrors = emptyList(),
            testResults = testResults,
            totalTests = exercise.testCases.size,
            totalExecutionTimeMs = elapsedMs(startNanos)
        )
    }

    private fun failedBeforeRunning(exercise: InterviewExercise, errors: List<String>, elapsedMs: Long) =
        ExerciseResultEvaluator.aggregate(
            exerciseId = exercise.id,
            compiled = false,
            compilationErrors = errors,
            testResults = emptyList(),
            totalTests = exercise.testCases.size,
            totalExecutionTimeMs = elapsedMs
        )

    private fun runOneTestCase(
        testCase: ExerciseTestCase,
        classesDir: File,
        dexFile: File,
        workDir: File,
        timeoutMs: Long
    ): TestCaseResult {
        val optimizedDir = File(workDir, "dex-opt/${testCase.id}")
        val raw = TestCaseRunner.run(classesDir, dexFile, optimizedDir, testCase.input, timeoutMs)
        return ExerciseResultEvaluator.evaluate(testCase, raw)
    }

    private fun elapsedMs(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000
}
