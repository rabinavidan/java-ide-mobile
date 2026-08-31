package com.javaide.mobile.practice.catalog

import com.javaide.mobile.practice.model.Difficulty
import com.javaide.mobile.practice.model.InterviewExercise

/** Simple in-memory [ExerciseCatalog] over a fixed list — the whole app-wide set, or any smaller subset. */
class InMemoryExerciseCatalog(private val exercises: List<InterviewExercise>) : ExerciseCatalog {

    override fun getAll(): List<InterviewExercise> = exercises

    override fun findById(id: String): InterviewExercise? =
        exercises.firstOrNull { it.id == id }

    override fun findByCategory(categoryId: String): List<InterviewExercise> =
        exercises.filter { it.categoryId == categoryId }

    override fun findByDifficulty(difficulty: Difficulty): List<InterviewExercise> =
        exercises.filter { it.difficulty == difficulty }

    override fun findByPattern(pattern: String): List<InterviewExercise> =
        exercises.filter { pattern in it.patterns }

    override fun search(query: String): List<InterviewExercise> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return emptyList()

        return exercises.filter { exercise ->
            exercise.title.lowercase().contains(needle) ||
                exercise.tags.any { it.lowercase().contains(needle) } ||
                exercise.patterns.any { it.lowercase().contains(needle) }
        }
    }
}
