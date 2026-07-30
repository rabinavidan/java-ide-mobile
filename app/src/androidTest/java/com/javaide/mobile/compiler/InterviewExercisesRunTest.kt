package com.javaide.mobile.compiler

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs classic coding-interview-style exercises through the full on-device pipeline
 * (compile -> dex -> execute) and checks the captured output against a known-correct value,
 * using the same AndroidJarProvider/JavaCompiler/Dexer/JavaRunner path the app itself uses.
 */
@RunWith(AndroidJUnit4::class)
class InterviewExercisesRunTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var androidJar: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        androidJar = AndroidJarProvider.get(context)
    }

    @Test fun fizzBuzz() = assertRunsWithExpectedOutput(InterviewExercises.FIZZ_BUZZ)

    @Test fun fibonacci() = assertRunsWithExpectedOutput(InterviewExercises.FIBONACCI)

    @Test fun twoSum() = assertRunsWithExpectedOutput(InterviewExercises.TWO_SUM)

    @Test fun isPalindrome() = assertRunsWithExpectedOutput(InterviewExercises.IS_PALINDROME)

    @Test fun binarySearch() = assertRunsWithExpectedOutput(InterviewExercises.BINARY_SEARCH)

    @Test fun groupAnagrams() = assertRunsWithExpectedOutput(InterviewExercises.GROUP_ANAGRAMS)

    private fun assertRunsWithExpectedOutput(exercise: InterviewExercise) {
        val projectDir = tempFolder.newFolder(exercise.className)
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "${exercise.className}.java").writeText(exercise.source)

        val classesDir = File(projectDir, "classes")
        val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar)
        assertTrue("Compilation failed for ${exercise.className}:\n${compileResult.log}", compileResult.success)

        val dexDir = File(projectDir, "dex")
        val dexResult = Dexer.dex(classesDir, dexDir, androidJar)
        assertTrue("Dexing failed for ${exercise.className}:\n${dexResult.log}", dexResult.success)

        val optimizedDir = File(projectDir, "dex-opt")
        val runResult = JavaRunner.run(classesDir, File(dexDir, "classes.dex"), optimizedDir)
        assertTrue("Run failed for ${exercise.className}:\n${runResult.output}", runResult.success)
        assertEquals(exercise.expectedOutput, runResult.output.trim())
    }
}
