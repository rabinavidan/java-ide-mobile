package com.javaide.mobile.compiler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PracticeCategoriesTest {

    @Test
    fun allCategoriesCoverEveryExerciseExactlyOnce() {
        val fromCategories = PracticeCategories.ALL.flatMap { it.exercises }

        assertEquals(InterviewExercises.ALL.size, fromCategories.size)
        assertEquals(InterviewExercises.ALL.toSet(), fromCategories.toSet())
        // "exactly once": no duplicates once flattened across every category
        assertEquals(fromCategories.size, fromCategories.distinct().size)
    }

    @Test
    fun tenCategoriesExist() {
        assertEquals(10, PracticeCategories.ALL.size)
    }

    @Test
    fun displayTitleSplitsCamelCaseKeepingAcronymsTogether() {
        assertEquals("Fizz Buzz", PracticeCategories.displayTitle(InterviewExercises.FIZZ_BUZZ))
        assertEquals("Graph BFS", PracticeCategories.displayTitle(InterviewExercises.GRAPH_BFS))
        assertEquals("Is Same Tree", PracticeCategories.displayTitle(InterviewExercises.IS_SAME_TREE))
        assertEquals("Min Stack", PracticeCategories.displayTitle(InterviewExercises.MIN_STACK))
    }

    @Test
    fun findLocatesExerciseByClassName() {
        assertEquals(InterviewExercises.FIZZ_BUZZ, PracticeCategories.find("FizzBuzz"))
        assertEquals(InterviewExercises.GRAPH_BFS, PracticeCategories.find("GraphBFS"))
    }

    @Test
    fun findReturnsNullForUnknownClassName() {
        assertNull(PracticeCategories.find("NoSuchExercise"))
    }
}
