package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises as Legacy
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.InterviewExercise

/**
 * Fundamentals topic (Milestone 3). Content is migrated from the legacy catalog via
 * [LegacyExerciseMigration] — see that class's doc for what's real vs. placeholder in the
 * result. Future milestones (6+) can add newly-authored [InterviewExercise]s to [ALL] directly,
 * alongside or instead of the migrated ones.
 */
object FundamentalsExercises {
    private const val CATEGORY_TITLE = "Fundamentals"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val ALL: List<InterviewExercise> = listOf(
        Legacy.FIZZ_BUZZ,
        Legacy.FIBONACCI,
        Legacy.TWO_SUM,
        Legacy.IS_PALINDROME,
        Legacy.BINARY_SEARCH,
        Legacy.GROUP_ANAGRAMS
    ).map { LegacyExerciseMigration.migrate(it, CATEGORY_ID, CATEGORY_TITLE) }
}
