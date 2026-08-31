package com.javaide.mobile.practice.execution

/** The outcome of running one [com.javaide.mobile.practice.model.ExerciseTestCase]. */
data class TestCaseResult(
    val testCaseId: String,
    val passed: Boolean,
    val expected: String,
    val actual: String,
    val executionTimeMs: Long,
    val errorMessage: String?
)
