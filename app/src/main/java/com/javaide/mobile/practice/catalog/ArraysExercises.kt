package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises as Legacy
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.InterviewExercise

/** Arrays & Strings topic (Milestone 3) — see [FundamentalsExercises] for the migration note. */
object ArraysExercises {
    private const val CATEGORY_TITLE = "Arrays & Strings"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val ALL: List<InterviewExercise> = listOf(
        Legacy.MAX_SUB_ARRAY,
        Legacy.REVERSE_STRING,
        Legacy.VALID_ANAGRAM,
        Legacy.CONTAINS_DUPLICATE,
        Legacy.MOVE_ZEROES
    ).map { LegacyExerciseMigration.migrate(it, CATEGORY_ID, CATEGORY_TITLE) }
}
