package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises as Legacy
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.InterviewExercise

/** Linked Lists topic (Milestone 3) — see [FundamentalsExercises] for the migration note. */
object LinkedListExercises {
    private const val CATEGORY_TITLE = "Linked Lists"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val ALL: List<InterviewExercise> = listOf(
        Legacy.REVERSE_LINKED_LIST,
        Legacy.MERGE_TWO_SORTED_LISTS,
        Legacy.LINKED_LIST_HAS_CYCLE
    ).map { LegacyExerciseMigration.migrate(it, CATEGORY_ID, CATEGORY_TITLE) }
}
