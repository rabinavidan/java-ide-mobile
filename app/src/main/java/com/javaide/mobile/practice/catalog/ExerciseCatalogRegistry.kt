package com.javaide.mobile.practice.catalog

import com.javaide.mobile.practice.model.InterviewExercise

/** Every existing topic file's `ALL` list. File-level (not a member of [ExerciseCatalogRegistry]) so the object's own supertype delegation below can read it during initialization without a self-reference. */
private val catalogTopics: List<List<InterviewExercise>> = listOf(
    FundamentalsExercises.ALL,
    ArraysExercises.ALL,
    LinkedListExercises.ALL,
    StackQueueExercises.ALL,
    TreeExercises.ALL,
    GraphExercises.ALL,
    SortingSearchingExercises.ALL,
    RecursionBacktrackingExercises.ALL,
    DynamicProgrammingExercises.ALL,
    BitManipulationExercises.ALL,
    HashMapExercises.ALL
)

/**
 * App-wide [ExerciseCatalog]: every exercise from every topic file, combined. Replaces direct
 * references to the legacy `com.javaide.mobile.compiler.InterviewExercises.TWO_SUM`-style
 * constants (or `PracticeCategories.find(className)`) with catalog lookups —
 * `ExerciseCatalogRegistry.findById("fundamentals-two-sum")` — for any new code written against
 * the V2 model. Not yet wired into the live Practice UI: that cutover needs the execution engine
 * (Milestone 4) and starter/solution separation (Milestone 5) first, so
 * `com.javaide.mobile.compiler.PracticeCategories`/`InterviewExercises` remain the UI's data
 * source for now, unchanged.
 *
 * Adding a new topic file (e.g. once Milestone 6 introduces `HashMapExercises.kt`) means adding
 * its `ALL` list to `catalogTopics` above — nothing else needs to change, including anything that
 * reads the catalog through [ExerciseCatalog]'s operations rather than [topics] directly.
 */
object ExerciseCatalogRegistry : ExerciseCatalog by InMemoryExerciseCatalog(catalogTopics.flatten()) {
    /** The exercises grouped by topic file, in case a caller cares about that grouping rather than the flattened lookup surface. */
    val topics: List<List<InterviewExercise>> = catalogTopics
}
