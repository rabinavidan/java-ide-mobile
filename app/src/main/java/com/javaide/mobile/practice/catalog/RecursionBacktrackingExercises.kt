package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises as Legacy
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.InterviewExercise

/**
 * Recursion & Backtracking topic (Milestone 3) — see [FundamentalsExercises] for the migration
 * note. Milestone 9 later adds a larger, dedicated Backtracking category with harder content;
 * this file keeps today's smaller combined grouping until that split is warranted.
 */
object RecursionBacktrackingExercises {
    private const val CATEGORY_TITLE = "Recursion & Backtracking"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val ALL: List<InterviewExercise> = listOf(
        Legacy.FACTORIAL,
        Legacy.PERMUTATIONS,
        Legacy.SUBSETS
    ).map { LegacyExerciseMigration.migrate(it, CATEGORY_ID, CATEGORY_TITLE) }
}
