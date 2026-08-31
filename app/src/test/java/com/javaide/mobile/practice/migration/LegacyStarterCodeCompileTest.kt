package com.javaide.mobile.practice.migration

import com.javaide.mobile.compiler.JavaCompiler
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Proves every hand-authored starter stub in [LegacyStarterCode] actually compiles, against the
 * real ECJ compiler ([JavaCompiler]) — the same guarantee `InterviewExercisesCompileDexTest`
 * gives the legacy reference solutions. A starter that doesn't compile as-authored would be a
 * broken first impression for whoever opens that exercise, so this runs for all 30, not a sample.
 */
class LegacyStarterCodeCompileTest {

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
    fun `every starter stub compiles`() {
        LegacyStarterCode.BY_CLASS_NAME.forEach { (className, content) ->
            val projectDir = tempFolder.newFolder(className)
            val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
            File(javaDir, "$className.java").writeText(content.starterCode)

            val classesDir = File(projectDir, "classes")
            val result = JavaCompiler.compile(projectDir, classesDir, androidJar)
            assertTrue("starter code for $className failed to compile:\n${result.log}", result.success)
        }
    }

    @Test
    fun `there is a starter stub for every legacy exercise`() {
        val legacyClassNames = com.javaide.mobile.compiler.InterviewExercises.ALL.map { it.className }.toSet()
        assertTrue(
            "missing starter stubs for: ${legacyClassNames - LegacyStarterCode.BY_CLASS_NAME.keys}",
            LegacyStarterCode.BY_CLASS_NAME.keys.containsAll(legacyClassNames)
        )
    }
}
