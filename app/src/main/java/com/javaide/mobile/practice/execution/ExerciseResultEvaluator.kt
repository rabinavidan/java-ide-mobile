package com.javaide.mobile.practice.execution

import com.javaide.mobile.practice.model.ExerciseTestCase

/**
 * Pure comparison/aggregation logic for the challenge execution engine (Milestone 4) — no
 * Android dependency, so it's fully unit-testable on the JVM, unlike [TestCaseRunner] (needs
 * `DexClassLoader`) or the compile/dex/run parts of [ExerciseRunner].
 */
object ExerciseResultEvaluator {

    /** What [redactHiddenTests] replaces a hidden test's [TestCaseResult.expected] with. */
    const val HIDDEN_PLACEHOLDER = "(hidden)"

    fun evaluate(testCase: ExerciseTestCase, raw: RawExecution): TestCaseResult {
        val errorMessage = when {
            raw.timedOut -> "Execution timed out after ${raw.executionTimeMs}ms."
            raw.threw -> raw.stderr.ifBlank { "The program threw an exception." }
            else -> null
        }
        // A timeout or an uncaught exception always fails the test, regardless of what (if
        // anything) made it into stdout first -- a run that didn't complete cleanly isn't correct
        // just because a partial prefix happens to match.
        val passed = errorMessage == null && normalize(raw.stdout) == normalize(testCase.expectedOutput)

        return TestCaseResult(
            testCaseId = testCase.id,
            passed = passed,
            expected = testCase.expectedOutput,
            actual = raw.stdout,
            executionTimeMs = raw.executionTimeMs,
            errorMessage = errorMessage
        )
    }

    fun aggregate(
        exerciseId: String,
        compiled: Boolean,
        compilationErrors: List<String>,
        testResults: List<TestCaseResult>,
        totalTests: Int,
        totalExecutionTimeMs: Long
    ): ExerciseRunResult {
        val passedTests = testResults.count { it.passed }
        val passed = compiled && totalTests > 0 && passedTests == totalTests

        return ExerciseRunResult(
            exerciseId = exerciseId,
            compiled = compiled,
            passed = passed,
            passedTests = passedTests,
            totalTests = totalTests,
            compilationErrors = compilationErrors,
            testResults = testResults,
            totalExecutionTimeMs = totalExecutionTimeMs
        )
    }

    /**
     * For Interview Mode: a copy of [result] with hidden test cases' [TestCaseResult.expected]
     * replaced by [HIDDEN_PLACEHOLDER]. Only the expected (model) answer is redacted — [actual]
     * (what the user's own program printed), [passed], and [errorMessage] stay visible, since
     * those describe the user's own submission, not the protected answer.
     */
    fun redactHiddenTests(result: ExerciseRunResult, testCases: List<ExerciseTestCase>): ExerciseRunResult {
        val visibleById = testCases.associate { it.id to it.visible }
        val redacted = result.testResults.map { testResult ->
            val visible = visibleById[testResult.testCaseId] ?: true
            if (visible) testResult else testResult.copy(expected = HIDDEN_PLACEHOLDER)
        }
        return result.copy(testResults = redacted)
    }

    /**
     * Safe output normalization: unifies line endings, strips trailing whitespace per line, and
     * strips a trailing run of blank lines at the very end (the usual "does the program end with
     * an extra newline or not" noise). Deliberately does *not* trim leading whitespace, collapse
     * internal whitespace, or strip blank lines in the middle of the output -- those can be
     * meaningful, and hiding a real mismatch there would violate "do not hide incorrect output".
     */
    fun normalize(text: String): String {
        val unifiedLineEndings = text.replace("\r\n", "\n").replace("\r", "\n")
        val trimmedTrailingPerLine = unifiedLineEndings.lines().joinToString("\n") { it.trimEnd(' ', '\t') }
        return trimmedTrailingPerLine.trimEnd('\n')
    }
}
