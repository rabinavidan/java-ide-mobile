package com.javaide.mobile.practice.catalog

import com.javaide.mobile.practice.model.Difficulty
import com.javaide.mobile.practice.model.ExerciseExample
import com.javaide.mobile.practice.model.ExerciseTestCase
import com.javaide.mobile.practice.model.InterviewExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises [InMemoryExerciseCatalog]'s filter operations against hand-built fixtures (rather
 * than the real, migrated catalog, whose placeholder content has no populated
 * difficulty/pattern variety yet) so `findByDifficulty`/`findByPattern`/`search` are proven
 * correct independently of what content currently exists.
 */
class InMemoryExerciseCatalogTest {

    private fun exercise(
        id: String,
        title: String,
        difficulty: Difficulty,
        patterns: Set<String> = emptySet(),
        tags: Set<String> = emptySet()
    ) = InterviewExercise(
        id = id,
        title = title,
        className = id.replace("-", "_"),
        categoryId = "test-category",
        difficulty = difficulty,
        description = "A test fixture.",
        constraints = emptyList(),
        examples = listOf(ExerciseExample("in", "out", "explanation")),
        starterCode = "class X {}",
        solutionCode = "class X {}",
        hints = emptyList(),
        testCases = listOf(ExerciseTestCase("t1", "in", "out", visible = true, description = "d")),
        timeComplexity = "O(1)",
        spaceComplexity = "O(1)",
        patterns = patterns,
        tags = tags,
        estimatedMinutes = 10
    )

    private val easyHashMap = exercise("easy-1", "Two Sum", Difficulty.EASY, patterns = setOf("hash-map"), tags = setOf("arrays"))
    private val mediumTwoPointers = exercise("medium-1", "Container With Most Water", Difficulty.MEDIUM, patterns = setOf("two-pointers"))
    private val hardHashMap = exercise("hard-1", "Word Break", Difficulty.HARD, patterns = setOf("hash-map", "dp"))

    private val catalog = InMemoryExerciseCatalog(listOf(easyHashMap, mediumTwoPointers, hardHashMap))

    @Test
    fun `findByDifficulty returns only matching exercises`() {
        assertEquals(listOf(easyHashMap), catalog.findByDifficulty(Difficulty.EASY))
        assertEquals(listOf(mediumTwoPointers), catalog.findByDifficulty(Difficulty.MEDIUM))
        assertEquals(listOf(hardHashMap), catalog.findByDifficulty(Difficulty.HARD))
    }

    @Test
    fun `findByPattern returns only exercises tagged with that exact pattern`() {
        assertEquals(setOf(easyHashMap, hardHashMap), catalog.findByPattern("hash-map").toSet())
        assertEquals(listOf(mediumTwoPointers), catalog.findByPattern("two-pointers"))
        assertTrue(catalog.findByPattern("does-not-exist").isEmpty())
    }

    @Test
    fun `search matches title case-insensitively`() {
        assertEquals(listOf(easyHashMap), catalog.search("two sum"))
        assertEquals(listOf(easyHashMap), catalog.search("TWO SUM"))
        assertEquals(listOf(easyHashMap), catalog.search("Two Sum"))
    }

    @Test
    fun `search matches tags and patterns too`() {
        assertEquals(listOf(easyHashMap), catalog.search("arrays"))
        assertEquals(setOf(easyHashMap, hardHashMap), catalog.search("hash-map").toSet())
    }

    @Test
    fun `search on a blank query returns nothing`() {
        assertTrue(catalog.search("").isEmpty())
        assertTrue(catalog.search("   ").isEmpty())
    }

    @Test
    fun `getAll and findById expose the full fixture set`() {
        assertEquals(3, catalog.getAll().size)
        assertEquals(easyHashMap, catalog.findById("easy-1"))
        assertEquals(null, catalog.findById("missing"))
    }
}
