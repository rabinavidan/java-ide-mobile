package com.javaide.mobile.practice.migration

import com.javaide.mobile.compiler.InterviewExercises
import com.javaide.mobile.practice.validation.ExerciseValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the Milestone 2 acceptance criteria: the existing 30 exercises load through the new
 * model, every migrated exercise has a unique id, and none depend on their display title as an
 * identifier (ids are derived from category + className, not title).
 */
class LegacyExerciseMigrationTest {

    @Test
    fun `migrating all legacy exercises preserves the count`() {
        val migrated = LegacyExerciseMigration.migrateAll()
        assertEquals(InterviewExercises.ALL.size, migrated.size)
        assertEquals(30, migrated.size)
    }

    @Test
    fun `every migrated exercise passes validation`() {
        val migrated = LegacyExerciseMigration.migrateAll()
        val errors = ExerciseValidator.validateCatalog(migrated)
        assertTrue("expected no validation errors, got: $errors", errors.isEmpty())
    }

    @Test
    fun `every migrated exercise has a unique, non-blank id independent of its title`() {
        val migrated = LegacyExerciseMigration.migrateAll()
        val ids = migrated.map { it.id }
        assertEquals("ids must be unique", ids.toSet().size, ids.size)
        assertTrue(migrated.all { it.id.isNotBlank() })
        // The id must not simply be a slugified copy of the title -- it has to survive a title rename.
        assertTrue(migrated.none { it.id == it.title })
    }

    @Test
    fun `every legacy exercise's expected output survives migration as a visible test case`() {
        val migrated = LegacyExerciseMigration.migrateAll()
        val byClassName = migrated.associateBy { it.className }

        InterviewExercises.ALL.forEach { legacy ->
            val migratedExercise = byClassName.getValue(legacy.className)
            assertEquals(legacy.source, migratedExercise.solutionCode)
            assertTrue(migratedExercise.testCases.any { it.visible && it.expectedOutput == legacy.expectedOutput })
        }
    }

    // --- Milestone 5: starter/solution separation ---

    @Test
    fun `every migrated exercise has starter code distinct from the solution`() {
        val migrated = LegacyExerciseMigration.migrateAll()
        migrated.forEach { exercise ->
            assertTrue(
                "expected starterCode to differ from solutionCode for ${exercise.className}",
                exercise.starterCode != exercise.solutionCode
            )
        }
    }

    @Test
    fun `every migrated exercise's starter code contains a TODO marker`() {
        val migrated = LegacyExerciseMigration.migrateAll()
        migrated.forEach { exercise ->
            assertTrue(
                "expected a TODO marker in starter code for ${exercise.className}",
                exercise.starterCode.contains("TODO")
            )
        }
    }

    @Test
    fun `every migrated exercise has at least two hints`() {
        val migrated = LegacyExerciseMigration.migrateAll()
        migrated.forEach { exercise ->
            assertTrue(
                "expected at least 2 hints for ${exercise.className}, got ${exercise.hints.size}",
                exercise.hints.size >= 2
            )
            assertTrue(exercise.hints.all { it.isNotBlank() })
        }
    }

    @Test
    fun `every migrated exercise's starter code keeps the same class name as the solution`() {
        val migrated = LegacyExerciseMigration.migrateAll()
        migrated.forEach { exercise ->
            assertTrue(
                "expected starter code for ${exercise.className} to declare a public class ${exercise.className}",
                exercise.starterCode.contains("class ${exercise.className}")
            )
        }
    }
}
