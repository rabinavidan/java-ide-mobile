package com.javaide.mobile.practice.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.javaide.mobile.compiler.AndroidJarProvider
import com.javaide.mobile.practice.execution.ExerciseRunner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

/**
 * Real, on-device proof that every [HashMapExercises] reference solution actually produces the
 * exact output its test cases (visible *and* hidden) expect — the same guarantee the legacy
 * catalog's `InterviewExercisesRunTest` gives, now exercised through the Milestone 4 engine
 * (`ExerciseRunner`/`TestCaseRunner`) instead of the single-shot `JavaRunner` path. Also confirms
 * the unimplemented starter code does *not* coincidentally pass ("solution does not appear
 * automatically" — Milestone 5's acceptance criterion, checked here with a real run rather than
 * just a source-string inequality).
 */
@RunWith(AndroidJUnit4::class)
class HashMapExercisesRunTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var androidJar: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        androidJar = AndroidJarProvider.get(context)
    }

    @Test
    fun everySolutionPassesAllOfItsOwnTestCasesIncludingHidden() {
        HashMapExercises.ALL.forEach { exercise ->
            val result = ExerciseRunner.run(exercise, exercise.solutionCode, tempFolder.newFolder("${exercise.className}-solution"), androidJar)

            assertTrue("${exercise.className} solution failed to compile:\n${result.compilationErrors}", result.compiled)
            assertTrue(
                "${exercise.className} solution did not pass all tests: ${result.testResults}",
                result.passed
            )
            assertTrue(result.testResults.size >= 3)
            assertTrue("expected a hidden test case among ${exercise.className}'s results", exercise.testCases.any { !it.visible })
        }
    }

    @Test
    fun theUnimplementedStarterDoesNotAlreadyPass() {
        HashMapExercises.ALL.forEach { exercise ->
            val result = ExerciseRunner.run(exercise, exercise.starterCode, tempFolder.newFolder("${exercise.className}-starter"), androidJar)

            assertTrue("${exercise.className} starter code should still compile", result.compiled)
            assertFalse("${exercise.className} starter code should not already pass -- the solution would be showing through", result.passed)
        }
    }
}
