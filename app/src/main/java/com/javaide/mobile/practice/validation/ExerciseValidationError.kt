package com.javaide.mobile.practice.validation

/**
 * A single validation failure from [ExerciseValidator]. Each case carries whatever identifies
 * the offending exercise so a caller (a failing test, a CI report) can point at exactly what's
 * wrong without re-deriving it from a plain string message.
 */
sealed class ExerciseValidationError(val message: String) {

    data class DuplicateId(val id: String) :
        ExerciseValidationError("Duplicate exercise id: \"$id\"")

    data class DuplicateClassName(val className: String) :
        ExerciseValidationError("Duplicate exercise className: \"$className\"")

    data class InvalidClassName(val id: String, val className: String) :
        ExerciseValidationError("Exercise \"$id\" has an invalid Java class name: \"$className\"")

    data class MissingTestCases(val id: String) :
        ExerciseValidationError("Exercise \"$id\" has no test cases")

    data class MissingExamples(val id: String) :
        ExerciseValidationError("Exercise \"$id\" has no examples")

    data class BlankDescription(val id: String) :
        ExerciseValidationError("Exercise \"$id\" has a blank description")

    data class BlankStarterCode(val id: String) :
        ExerciseValidationError("Exercise \"$id\" has blank starter code")

    data class BlankSolutionCode(val id: String) :
        ExerciseValidationError("Exercise \"$id\" has blank solution code")

    data class InvalidEstimatedMinutes(val id: String, val minutes: Int) :
        ExerciseValidationError("Exercise \"$id\" has a non-positive estimatedMinutes: $minutes")
}
