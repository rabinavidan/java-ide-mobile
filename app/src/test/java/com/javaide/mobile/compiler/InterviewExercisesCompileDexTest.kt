package com.javaide.mobile.compiler

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Verifies the compile -> dex steps of the pipeline against classic coding-interview-style
 * exercises. Runs as a plain JVM test (no device needed). Actually *executing* the exercises
 * needs a real Android runtime (DexClassLoader) and is covered by the instrumented
 * counterpart, InterviewExercisesRunTest.
 */
class InterviewExercisesCompileDexTest {

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

    @Test fun fizzBuzz() = assertCompilesAndDexes(InterviewExercises.FIZZ_BUZZ)

    @Test fun fibonacci() = assertCompilesAndDexes(InterviewExercises.FIBONACCI)

    @Test fun twoSum() = assertCompilesAndDexes(InterviewExercises.TWO_SUM)

    @Test fun isPalindrome() = assertCompilesAndDexes(InterviewExercises.IS_PALINDROME)

    @Test fun binarySearch() = assertCompilesAndDexes(InterviewExercises.BINARY_SEARCH)

    @Test fun groupAnagrams() = assertCompilesAndDexes(InterviewExercises.GROUP_ANAGRAMS)

    private fun assertCompilesAndDexes(exercise: InterviewExercise) {
        val projectDir = tempFolder.newFolder(exercise.className)
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "${exercise.className}.java").writeText(exercise.source)

        val classesDir = File(projectDir, "classes")
        val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar)
        assertTrue("Compilation failed for ${exercise.className}:\n${compileResult.log}", compileResult.success)

        val dexDir = File(projectDir, "dex")
        val dexResult = Dexer.dex(classesDir, dexDir, androidJar)
        assertTrue("Dexing failed for ${exercise.className}:\n${dexResult.log}", dexResult.success)
        assertTrue(File(dexDir, "classes.dex").isFile)
    }
}
