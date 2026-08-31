package com.javaide.mobile.practice.execution

/** The full outcome of running one [com.javaide.mobile.practice.model.InterviewExercise]'s code against all of its test cases. */
data class ExerciseRunResult(
    val exerciseId: String,
    val compiled: Boolean,
    val passed: Boolean,
    val passedTests: Int,
    val totalTests: Int,
    val compilationErrors: List<String>,
    val testResults: List<TestCaseResult>,
    val totalExecutionTimeMs: Long
)
