package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.InterviewExercises
import com.javaide.mobile.compiler.PracticeCategories
import com.javaide.mobile.practice.migration.LegacyExerciseMigration
import com.javaide.mobile.practice.validation.ExerciseValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Milestone 3 acceptance checks against the real, app-wide catalog: all legacy exercises are
 * registered, no duplicate ids/classNames, categories contain valid exercises, and the exercise
 * count is derived (never hardcoded) from the catalog itself. Deliberately does not assert the
 * catalog's total size anywhere in this file -- the catalog is expected to keep growing as new
 * topic files (Milestone 6+) add content, so a hardcoded total would break on every addition.
 */
class ExerciseCatalogRegistryTest {

    @Test
    fun `every legacy exercise is registered exactly once`() {
        val all = ExerciseCatalogRegistry.getAll()
        assertTrue(all.size >= InterviewExercises.ALL.size)

        val registeredClassNames = all.map { it.className }.toSet()
        InterviewExercises.ALL.forEach { legacy ->
            assertTrue("missing ${legacy.className}", legacy.className in registeredClassNames)
        }
    }

    @Test
    fun `no duplicate ids or class names`() {
        val errors = ExerciseValidator.validateCatalog(ExerciseCatalogRegistry.getAll())
        assertTrue("expected no validation errors, got: $errors", errors.isEmpty())
    }

    @Test
    fun `findById resolves every registered exercise`() {
        ExerciseCatalogRegistry.getAll().forEach { exercise ->
            assertEquals(exercise, ExerciseCatalogRegistry.findById(exercise.id))
        }
        assertEquals(null, ExerciseCatalogRegistry.findById("does-not-exist"))
    }

    @Test
    fun `every legacy category contains only valid, correctly-tagged exercises`() {
        PracticeCategories.ALL.forEach { category ->
            val categoryId = LegacyExerciseMigration.categoryIdFor(category.title)
            val exercisesInCategory = ExerciseCatalogRegistry.findByCategory(categoryId)

            assertEquals(
                "category \"${category.title}\" ($categoryId) should have ${category.exercises.size} exercises",
                category.exercises.size,
                exercisesInCategory.size
            )
            assertTrue(exercisesInCategory.all { it.categoryId == categoryId })
            assertTrue(exercisesInCategory.all { ExerciseValidator.validate(it).isEmpty() })
        }
    }

    @Test
    fun `topics list matches the flattened catalog`() {
        assertEquals(ExerciseCatalogRegistry.getAll().size, ExerciseCatalogRegistry.topics.flatten().size)
        assertTrue(ExerciseCatalogRegistry.topics.size >= 10)
    }

    @Test
    fun `search is case-insensitive and finds migrated exercises by tag`() {
        val lower = ExerciseCatalogRegistry.search("legacy-migration")
        val upper = ExerciseCatalogRegistry.search("LEGACY-MIGRATION")
        val mixed = ExerciseCatalogRegistry.search("Legacy-Migration")

        assertEquals(30, lower.size)
        assertEquals(lower.toSet(), upper.toSet())
        assertEquals(lower.toSet(), mixed.toSet())
    }

    @Test
    fun `search finds an exercise by its title, case-insensitively`() {
        val result = ExerciseCatalogRegistry.search("fizz buzz")
        assertNotNull(result.firstOrNull { it.className == "FizzBuzz" })
    }
}
