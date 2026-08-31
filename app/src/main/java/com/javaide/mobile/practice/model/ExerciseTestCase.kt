package com.javaide.mobile.practice.model

/**
 * One structured test case for an exercise. [visible] tests are shown to the user with their
 * expected output (Practice/Learn Mode); non-visible ("hidden") tests are run the same way but
 * their [expectedOutput] must not be surfaced to the user — see Interview Mode requirements.
 */
data class ExerciseTestCase(
    val id: String,
    val input: String,
    val expectedOutput: String,
    val visible: Boolean,
    val description: String
)
