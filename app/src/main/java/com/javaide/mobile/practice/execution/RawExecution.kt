package com.javaide.mobile.practice.execution

/**
 * The unprocessed result of one [TestCaseRunner] invocation, before [ExerciseResultEvaluator]
 * turns it into a [TestCaseResult] by comparing [stdout] against a test case's expected output.
 * stdout/stderr are kept separate per the Milestone 4 requirement ("capture stdout and stderr
 * separately") — [stderr] is treated as diagnostic (a stack trace, mainly) rather than part of
 * what gets compared.
 */
data class RawExecution(
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long,
    val threw: Boolean,
    val timedOut: Boolean
)
