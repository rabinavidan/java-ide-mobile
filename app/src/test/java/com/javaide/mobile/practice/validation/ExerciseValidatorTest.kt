package com.javaide.mobile.practice.validation

import com.javaide.mobile.practice.model.Difficulty
import com.javaide.mobile.practice.model.ExerciseExample
import com.javaide.mobile.practice.model.ExerciseTestCase
import com.javaide.mobile.practice.model.InterviewExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseValidatorTest {

    private fun validExercise(id: String = "arrays-two-sum", className: String = "TwoSum") = InterviewExercise(
        id = id,
        title = "Two Sum",
        className = className,
        categoryId = "arrays",
        difficulty = Difficulty.EASY,
        description = "Given an array of integers, return indices of the two numbers that add up to a target.",
        constraints = listOf("2 <= nums.length <= 10^4"),
        examples = listOf(ExerciseExample(input = "[2,7,11,15], target=9", output = "[0,1]", explanation = "nums[0] + nums[1] == 9")),
        starterCode = "public class TwoSum { /* TODO */ }",
        solutionCode = "public class TwoSum { /* solution */ }",
        hints = listOf("Try a HashMap of value -> index."),
        testCases = listOf(ExerciseTestCase(id = "t1", input = "[2,7,11,15], 9", expectedOutput = "[0,1]", visible = true, description = "basic case")),
        timeComplexity = "O(n)",
        spaceComplexity = "O(n)",
        patterns = setOf("hash-map"),
        tags = setOf("arrays"),
        estimatedMinutes = 15
    )

    @Test
    fun `a fully-populated exercise has no errors`() {
        assertTrue(ExerciseValidator.validate(validExercise()).isEmpty())
    }

    @Test
    fun `invalid class names are rejected`() {
        val invalidNames = listOf("2Sum", "Two-Sum", "class", "", "Two Sum")
        for (name in invalidNames) {
            val errors = ExerciseValidator.validate(validExercise(className = name))
            assertTrue("expected InvalidClassName for \"$name\", got $errors", errors.any { it is ExerciseValidationError.InvalidClassName })
        }
    }

    @Test
    fun `valid class names are accepted`() {
        for (name in listOf("TwoSum", "_Private", "\$Generated", "A")) {
            val errors = ExerciseValidator.validate(validExercise(className = name))
            assertTrue("did not expect InvalidClassName for \"$name\", got $errors", errors.none { it is ExerciseValidationError.InvalidClassName })
        }
    }

    @Test
    fun `missing test cases are rejected`() {
        val errors = ExerciseValidator.validate(validExercise().copy(testCases = emptyList()))
        assertTrue(errors.any { it is ExerciseValidationError.MissingTestCases })
    }

    @Test
    fun `missing examples are rejected`() {
        val errors = ExerciseValidator.validate(validExercise().copy(examples = emptyList()))
        assertTrue(errors.any { it is ExerciseValidationError.MissingExamples })
    }

    @Test
    fun `missing descriptions are rejected`() {
        for (blank in listOf("", "   ")) {
            val errors = ExerciseValidator.validate(validExercise().copy(description = blank))
            assertTrue(errors.any { it is ExerciseValidationError.BlankDescription })
        }
    }

    @Test
    fun `empty starter code is rejected`() {
        val errors = ExerciseValidator.validate(validExercise().copy(starterCode = ""))
        assertTrue(errors.any { it is ExerciseValidationError.BlankStarterCode })
    }

    @Test
    fun `empty solution code is rejected`() {
        val errors = ExerciseValidator.validate(validExercise().copy(solutionCode = "   "))
        assertTrue(errors.any { it is ExerciseValidationError.BlankSolutionCode })
    }

    @Test
    fun `invalid estimated time is rejected`() {
        for (minutes in listOf(0, -5)) {
            val errors = ExerciseValidator.validate(validExercise().copy(estimatedMinutes = minutes))
            assertTrue("expected InvalidEstimatedMinutes for $minutes", errors.any { it is ExerciseValidationError.InvalidEstimatedMinutes })
        }
    }

    @Test
    fun `positive estimated time is accepted`() {
        val errors = ExerciseValidator.validate(validExercise().copy(estimatedMinutes = 1))
        assertTrue(errors.none { it is ExerciseValidationError.InvalidEstimatedMinutes })
    }

    @Test
    fun `duplicate IDs are rejected at the catalog level`() {
        val exercises = listOf(
            validExercise(id = "dup-id", className = "ClassOne"),
            validExercise(id = "dup-id", className = "ClassTwo")
        )
        val errors = ExerciseValidator.validateCatalog(exercises)
        assertTrue(errors.any { it is ExerciseValidationError.DuplicateId && it.id == "dup-id" })
    }

    @Test
    fun `duplicate class names are rejected at the catalog level`() {
        val exercises = listOf(
            validExercise(id = "id-1", className = "SameClass"),
            validExercise(id = "id-2", className = "SameClass")
        )
        val errors = ExerciseValidator.validateCatalog(exercises)
        assertTrue(errors.any { it is ExerciseValidationError.DuplicateClassName && it.className == "SameClass" })
    }

    @Test
    fun `a catalog of unique valid exercises has no errors`() {
        val exercises = listOf(
            validExercise(id = "id-1", className = "ClassOne"),
            validExercise(id = "id-2", className = "ClassTwo")
        )
        assertEquals(emptyList<ExerciseValidationError>(), ExerciseValidator.validateCatalog(exercises))
    }
}
