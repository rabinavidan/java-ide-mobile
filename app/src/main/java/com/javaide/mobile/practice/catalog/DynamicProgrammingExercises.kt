package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises as Legacy
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.InterviewExercise

/** Dynamic Programming topic (Milestone 3) — see [FundamentalsExercises] for the migration note. */
object DynamicProgrammingExercises {
    private const val CATEGORY_TITLE = "Dynamic Programming"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val ALL: List<InterviewExercise> = listOf(
        Legacy.CLIMBING_STAIRS,
        Legacy.COIN_CHANGE
    ).map { LegacyExerciseMigration.migrate(it, CATEGORY_ID, CATEGORY_TITLE) }
}
