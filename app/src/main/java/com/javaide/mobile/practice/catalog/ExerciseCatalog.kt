package com.javaide.mobile.practice.catalog

import com.javaide.mobile.practice.model.Difficulty
import com.javaide.mobile.practice.model.InterviewExercise

/**
 * Read-only lookup surface over a set of [InterviewExercise]s. [ExerciseCatalogRegistry] is the
 * app-wide singleton instance (all 30 migrated exercises, split across topic files); tests and
 * future scoped views (e.g. an Interview Mode session's chosen subset) can use
 * [InMemoryExerciseCatalog] directly against a smaller list.
 */
interface ExerciseCatalog {

    fun getAll(): List<InterviewExercise>

    fun findById(id: String): InterviewExercise?

    fun findByCategory(categoryId: String): List<InterviewExercise>

    fun findByDifficulty(difficulty: Difficulty): List<InterviewExercise>

    /** Exercises tagged with [pattern] in [InterviewExercise.patterns] (exact, case-sensitive match). */
    fun findByPattern(pattern: String): List<InterviewExercise>

    /** Case-insensitive substring match against title, tags, and patterns. Blank query returns no results. */
    fun search(query: String): List<InterviewExercise>
}
