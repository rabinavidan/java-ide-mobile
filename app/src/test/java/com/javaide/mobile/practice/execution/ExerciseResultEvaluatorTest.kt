package com.javaide.mobile.practice.execution

import com.javaide.mobile.practice.model.ExerciseTestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseResultEvaluatorTest {

    private fun testCase(id: String = "t1", input: String = "", expected: String, visible: Boolean = true) =
        ExerciseTestCase(id = id, input = input, expectedOutput = expected, visible = visible, description = "d")

    private fun raw(stdout: String = "", stderr: String = "", executionTimeMs: Long = 5, threw: Boolean = false, timedOut: Boolean = false) =
        RawExecution(stdout, stderr, executionTimeMs, threw, timedOut)

    // --- normalize() ---

    @Test
    fun `normalize unifies line endings`() {
        assertEquals(
            ExerciseResultEvaluator.normalize("a\nb\nc"),
            ExerciseResultEvaluator.normalize("a\r\nb\r\nc")
        )
        assertEquals(
            ExerciseResultEvaluator.normalize("a\nb\nc"),
            ExerciseResultEvaluator.normalize("a\rb\rc")
        )
    }

    @Test
    fun `normalize strips trailing whitespace per line but keeps leading and internal whitespace`() {
        assertEquals("a\nb", ExerciseResultEvaluator.normalize("a  \nb\t\t"))
        assertEquals("  a\nb   c", ExerciseResultEvaluator.normalize("  a\nb   c"))
    }

    @Test
    fun `normalize strips a trailing run of blank lines but not blank lines in the middle`() {
        assertEquals("a\nb", ExerciseResultEvaluator.normalize("a\nb\n\n\n"))
        assertEquals("a\n\nb", ExerciseResultEvaluator.normalize("a\n\nb\n"))
    }

    @Test
    fun `normalize does not collapse internal whitespace or hide extra content`() {
        assertFalse(ExerciseResultEvaluator.normalize("5") == ExerciseResultEvaluator.normalize("5\nDEBUG: extra"))
        assertFalse(ExerciseResultEvaluator.normalize("a b") == ExerciseResultEvaluator.normalize("a  b"))
    }

    // --- evaluate(): correct / incorrect solution ---

    @Test
    fun `evaluate passes when normalized stdout matches expected`() {
        val result = ExerciseResultEvaluator.evaluate(testCase(expected = "5"), raw(stdout = "5\n"))
        assertTrue(result.passed)
        assertEquals("5", result.expected)
        assertEquals("5\n", result.actual)
        assertEquals(null, result.errorMessage)
    }

    @Test
    fun `evaluate fails on a wrong result`() {
        val result = ExerciseResultEvaluator.evaluate(testCase(expected = "5"), raw(stdout = "4"))
        assertFalse(result.passed)
        assertEquals(null, result.errorMessage)
    }

    @Test
    fun `evaluate fails when the program prints extra console output`() {
        val result = ExerciseResultEvaluator.evaluate(testCase(expected = "5"), raw(stdout = "5\nDEBUG: extra"))
        assertFalse(result.passed)
    }

    @Test
    fun `evaluate passes on matching empty output`() {
        val result = ExerciseResultEvaluator.evaluate(testCase(expected = ""), raw(stdout = ""))
        assertTrue(result.passed)
    }

    @Test
    fun `evaluate fails when output is empty but content was expected`() {
        val result = ExerciseResultEvaluator.evaluate(testCase(expected = "5"), raw(stdout = ""))
        assertFalse(result.passed)
    }

    // --- evaluate(): runtime exception / timeout ---

    @Test
    fun `evaluate fails a run that threw, even if stdout happens to match`() {
        val result = ExerciseResultEvaluator.evaluate(
            testCase(expected = "5"),
            raw(stdout = "5", stderr = "java.lang.RuntimeException: boom", threw = true)
        )
        assertFalse(result.passed)
        assertEquals("java.lang.RuntimeException: boom", result.errorMessage)
    }

    @Test
    fun `evaluate reports a timeout as a failure with a descriptive error`() {
        val result = ExerciseResultEvaluator.evaluate(
            testCase(expected = "5"),
            raw(stdout = "", executionTimeMs = 5000, timedOut = true)
        )
        assertFalse(result.passed)
        assertTrue(result.errorMessage!!.contains("timed out", ignoreCase = true))
    }

    // --- aggregate() ---

    @Test
    fun `aggregate counts passed tests and marks the exercise passed only when all pass`() {
        val results = listOf(
            TestCaseResult("t1", passed = true, expected = "a", actual = "a", executionTimeMs = 1, errorMessage = null),
            TestCaseResult("t2", passed = true, expected = "b", actual = "b", executionTimeMs = 1, errorMessage = null)
        )
        val run = ExerciseResultEvaluator.aggregate("ex-1", compiled = true, compilationErrors = emptyList(), testResults = results, totalTests = 2, totalExecutionTimeMs = 10)
        assertTrue(run.passed)
        assertEquals(2, run.passedTests)
        assertEquals(2, run.totalTests)
    }

    @Test
    fun `aggregate marks the exercise not passed when some tests fail`() {
        val results = listOf(
            TestCaseResult("t1", passed = true, expected = "a", actual = "a", executionTimeMs = 1, errorMessage = null),
            TestCaseResult("t2", passed = false, expected = "b", actual = "c", executionTimeMs = 1, errorMessage = null)
        )
        val run = ExerciseResultEvaluator.aggregate("ex-1", compiled = true, compilationErrors = emptyList(), testResults = results, totalTests = 2, totalExecutionTimeMs = 10)
        assertFalse(run.passed)
        assertEquals(1, run.passedTests)
        assertEquals(2, run.totalTests)
    }

    @Test
    fun `aggregate marks the exercise not passed when it failed to compile`() {
        val run = ExerciseResultEvaluator.aggregate("ex-1", compiled = false, compilationErrors = listOf("syntax error"), testResults = emptyList(), totalTests = 3, totalExecutionTimeMs = 5)
        assertFalse(run.passed)
        assertEquals(0, run.passedTests)
        assertEquals(3, run.totalTests)
        assertEquals(listOf("syntax error"), run.compilationErrors)
    }

    // --- redactHiddenTests() ---

    @Test
    fun `redactHiddenTests hides only the expected value of hidden test cases`() {
        val testCases = listOf(
            testCase(id = "visible-1", expected = "5", visible = true),
            testCase(id = "hidden-1", expected = "secret-answer", visible = false)
        )
        val results = listOf(
            TestCaseResult("visible-1", passed = true, expected = "5", actual = "5", executionTimeMs = 1, errorMessage = null),
            TestCaseResult("hidden-1", passed = false, expected = "secret-answer", actual = "wrong-guess", executionTimeMs = 1, errorMessage = null)
        )
        val run = ExerciseResultEvaluator.aggregate("ex-1", compiled = true, compilationErrors = emptyList(), testResults = results, totalTests = 2, totalExecutionTimeMs = 10)

        val redacted = ExerciseResultEvaluator.redactHiddenTests(run, testCases)

        val visibleResult = redacted.testResults.first { it.testCaseId == "visible-1" }
        val hiddenResult = redacted.testResults.first { it.testCaseId == "hidden-1" }

        assertEquals("5", visibleResult.expected)
        assertEquals(ExerciseResultEvaluator.HIDDEN_PLACEHOLDER, hiddenResult.expected)
        // The user's own output and pass/fail status are never hidden -- only the model answer is.
        assertEquals("wrong-guess", hiddenResult.actual)
        assertFalse(hiddenResult.passed)
        // Pass/fail counts must not change just because presentation redacted some content.
        assertEquals(run.passedTests, redacted.passedTests)
        assertEquals(run.totalTests, redacted.totalTests)
    }
}
