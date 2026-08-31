package com.javaide.mobile.practice.validation

import com.javaide.mobile.practice.model.InterviewExercise

/**
 * Structural (pure-data) validation for [InterviewExercise], per Milestone 2 of the Interview
 * Practice Expansion plan (docs/ROADMAP.md). Deliberately does *not* attempt the two
 * compile-time rules from that milestone's list ("starter code must compile after
 * implementation", "solution code must compile and pass all tests") — those need the on-device
 * ECJ/D8 toolchain (see [com.javaide.mobile.compiler.JavaCompiler]/[com.javaide.mobile.compiler.Dexer])
 * and are exercised the same way the existing catalog already is, via a compiler-integration
 * test suite (see `InterviewExercisesCompileDexTest`/`InterviewExercisesRunTest`), not a
 * lightweight synchronous validator.
 */
object ExerciseValidator {

    private val javaKeywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null"
    )

    private val validClassNamePattern = Regex("^[A-Za-z_$][A-Za-z0-9_$]*$")

    /** Validates a single exercise in isolation — everything except catalog-wide uniqueness. */
    fun validate(exercise: InterviewExercise): List<ExerciseValidationError> {
        val errors = mutableListOf<ExerciseValidationError>()

        if (!isValidClassName(exercise.className)) {
            errors += ExerciseValidationError.InvalidClassName(exercise.id, exercise.className)
        }
        if (exercise.testCases.isEmpty()) {
            errors += ExerciseValidationError.MissingTestCases(exercise.id)
        }
        if (exercise.examples.isEmpty()) {
            errors += ExerciseValidationError.MissingExamples(exercise.id)
        }
        if (exercise.description.isBlank()) {
            errors += ExerciseValidationError.BlankDescription(exercise.id)
        }
        if (exercise.starterCode.isBlank()) {
            errors += ExerciseValidationError.BlankStarterCode(exercise.id)
        }
        if (exercise.solutionCode.isBlank()) {
            errors += ExerciseValidationError.BlankSolutionCode(exercise.id)
        }
        if (exercise.estimatedMinutes <= 0) {
            errors += ExerciseValidationError.InvalidEstimatedMinutes(exercise.id, exercise.estimatedMinutes)
        }

        return errors
    }

    /** Validates a whole catalog: every exercise individually, plus cross-exercise uniqueness of id/className. */
    fun validateCatalog(exercises: List<InterviewExercise>): List<ExerciseValidationError> {
        val errors = mutableListOf<ExerciseValidationError>()

        exercises.groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { errors += ExerciseValidationError.DuplicateId(it) }

        exercises.groupBy { it.className }
            .filterValues { it.size > 1 }
            .keys
            .forEach { errors += ExerciseValidationError.DuplicateClassName(it) }

        exercises.forEach { errors += validate(it) }

        return errors
    }

    private fun isValidClassName(className: String): Boolean =
        className.isNotBlank() &&
            validClassNamePattern.matches(className) &&
            className !in javaKeywords
}
