package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises as Legacy
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.InterviewExercise

/**
 * Sorting & Searching topic (Milestone 3) — see [FundamentalsExercises] for the migration note.
 * Not one of the target structure's named files (that list anticipates the pattern-specific
 * categories Milestones 6-9 add, e.g. a dedicated BinarySearchExercises.kt); this keeps today's
 * actual "Sorting & Searching" grouping intact until that content exists to split it further.
 */
object SortingSearchingExercises {
    private const val CATEGORY_TITLE = "Sorting & Searching"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val ALL: List<InterviewExercise> = listOf(
        Legacy.MERGE_SORT,
        Legacy.SEARCH_INSERT_POSITION
    ).map { LegacyExerciseMigration.migrate(it, CATEGORY_ID, CATEGORY_TITLE) }
}
