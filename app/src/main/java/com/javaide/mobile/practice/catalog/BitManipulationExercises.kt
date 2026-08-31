package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises as Legacy
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.InterviewExercise

/** Bit Manipulation topic (Milestone 3) — see [FundamentalsExercises] for the migration note. */
object BitManipulationExercises {
    private const val CATEGORY_TITLE = "Bit Manipulation"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val ALL: List<InterviewExercise> = listOf(
        Legacy.SINGLE_NUMBER
    ).map { LegacyExerciseMigration.migrate(it, CATEGORY_ID, CATEGORY_TITLE) }
}
