package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises as Legacy
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.InterviewExercise

/** Stacks & Queues topic (Milestone 3) — see [FundamentalsExercises] for the migration note. */
object StackQueueExercises {
    private const val CATEGORY_TITLE = "Stacks & Queues"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val ALL: List<InterviewExercise> = listOf(
        Legacy.VALID_PARENTHESES,
        Legacy.MIN_STACK
    ).map { LegacyExerciseMigration.migrate(it, CATEGORY_ID, CATEGORY_TITLE) }
}
