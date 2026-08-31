package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises as Legacy
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.model.InterviewExercise

/** Trees topic (Milestone 3) — see [FundamentalsExercises] for the migration note. */
object TreeExercises {
    private const val CATEGORY_TITLE = "Trees"
    private val CATEGORY_ID = LegacyExerciseMigration.categoryIdFor(CATEGORY_TITLE)

    val ALL: List<InterviewExercise> = listOf(
        Legacy.TREE_INORDER_TRAVERSAL,
        Legacy.TREE_MAX_DEPTH,
        Legacy.IS_SAME_TREE,
        Legacy.TREE_LEVEL_ORDER
    ).map { LegacyExerciseMigration.migrate(it, CATEGORY_ID, CATEGORY_TITLE) }
}
