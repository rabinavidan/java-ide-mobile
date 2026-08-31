package com.javaide.mobile.practice.catalog

import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.practice.validation.ExerciseValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Milestone 6 (Section A) content checks for [HashMapExercises] — structural validity and real
 * compilability of both starter and solution code. Whether the solutions actually *pass* their
 * own test cases needs `DexClassLoader`, so that's verified separately, on-device, by
 * `HashMapExercisesRunTest` (androidTest).
 */
class HashMapExercisesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var androidJar: File

    @Before
    fun setUp() {
        val path = System.getProperty("android.jar.path")
            ?: error("android.jar.path system property not set; see app/build.gradle.kts")
        androidJar = File(path)
        check(androidJar.isFile) { "android.jar not found at $path" }
    }

    @Test
    fun `there are exactly six hash map exercises`() {
        assertEquals(6, HashMapExercises.ALL.size)
    }

    @Test
    fun `every exercise passes structural validation`() {
        val errors = ExerciseValidator.validateCatalog(HashMapExercises.ALL)
        assertTrue("expected no validation errors, got: $errors", errors.isEmpty())
    }

    @Test
    fun `every exercise has at least three test cases including a hidden one`() {
        HashMapExercises.ALL.forEach { exercise ->
            assertTrue(
                "expected >= 3 test cases for ${exercise.className}, got ${exercise.testCases.size}",
                exercise.testCases.size >= 3
            )
            assertTrue(
                "expected at least one hidden test case for ${exercise.className}",
                exercise.testCases.any { !it.visible }
            )
        }
    }

    @Test
    fun `every exercise has at least two examples and two hints`() {
        HashMapExercises.ALL.forEach { exercise ->
            assertTrue("expected >= 2 examples for ${exercise.className}", exercise.examples.size >= 2)
            assertTrue("expected >= 2 hints for ${exercise.className}", exercise.hints.size >= 2)
        }
    }

    @Test
    fun `starter code differs from the solution and contains a TODO marker`() {
        HashMapExercises.ALL.forEach { exercise ->
            assertTrue(exercise.starterCode != exercise.solutionCode)
            assertTrue(exercise.starterCode.contains("TODO"))
        }
    }

    @Test
    fun `both starter and solution code compile`() {
        HashMapExercises.ALL.forEach { exercise ->
            listOf("starter" to exercise.starterCode, "solution" to exercise.solutionCode).forEach { (kind, source) ->
                val projectDir = tempFolder.newFolder("${exercise.className}-$kind")
                val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
                File(javaDir, "${exercise.className}.java").writeText(source)

                val classesDir = File(projectDir, "classes")
                val result = JavaCompiler.compile(projectDir, classesDir, androidJar)
                assertTrue("$kind code for ${exercise.className} failed to compile:\n${result.log}", result.success)
            }
        }
    }
}
