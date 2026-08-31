package com.javaide.mobile.practice.model

/**
 * Practice domain model V2 (Milestone 2 of the Interview Practice Expansion plan — see
 * docs/ROADMAP.md). Distinct from, and not yet wired to, the existing
 * [com.javaide.mobile.compiler.InterviewExercise] that backs today's Practice UI; that legacy
 * model keeps working unchanged until the catalog/runner/UI milestones (3-5) migrate over to
 * this one. [com.javaide.mobile.practice.migration.LegacyExerciseMigration] converts the current
 * 30 legacy exercises into this shape so the new model and its [ExerciseValidator] can be
 * exercised against real data ahead of that cutover.
 */
data class InterviewExercise(
    val id: String,
    val title: String,
    val className: String,
    val categoryId: String,
    val difficulty: Difficulty,
    val description: String,
    val constraints: List<String>,
    val examples: List<ExerciseExample>,
    val starterCode: String,
    val solutionCode: String,
    val hints: List<String>,
    val testCases: List<ExerciseTestCase>,
    val timeComplexity: String,
    val spaceComplexity: String,
    val patterns: Set<String>,
    val tags: Set<String>,
    val estimatedMinutes: Int
)
