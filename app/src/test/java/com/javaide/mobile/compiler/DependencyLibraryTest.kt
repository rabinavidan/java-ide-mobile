package com.javaide.mobile.compiler

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * End-to-end proof that a third-party .jar dropped into a project's libs/ folder (see LibJars)
 * actually reaches both ECJ compilation and D8 dexing, mirroring how FileExplorerActivity wires
 * LibJars.jarsIn(projectDir) into JavaCompiler.compile()/Dexer.dex().
 */
class DependencyLibraryTest {

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

    private fun appProjectReferencingLib(): File {
        val projectDir = tempFolder.newFolder("app-project")
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "Main.java").writeText(
            """
            public class Main {
                public static void main(String[] args) {
                    System.out.println(testlib.Lib.greet());
                }
            }
            """.trimIndent()
        )
        return projectDir
    }

    @Test
    fun compileFailsWithoutTheLibraryJarOnTheClasspath() {
        val projectDir = appProjectReferencingLib()
        val classesDir = File(projectDir, "classes")

        val result = JavaCompiler.compile(projectDir, classesDir, androidJar)

        assertFalse(result.success)
    }

    @Test
    fun compileSucceedsWhenTheLibraryJarIsPassedAsALibJar() {
        val libJar = TestLibJarBuilder.build(tempFolder.newFolder("lib-work"), androidJar)
        val projectDir = appProjectReferencingLib()
        val classesDir = File(projectDir, "classes")

        val result = JavaCompiler.compile(projectDir, classesDir, androidJar, libJars = listOf(libJar))

        assertTrue(result.log, result.success)
    }

    @Test
    fun dexMergesTheLibraryJarsClassesIntoTheOutputDex() {
        val libJar = TestLibJarBuilder.build(tempFolder.newFolder("lib-work"), androidJar)
        val projectDir = appProjectReferencingLib()
        val classesDir = File(projectDir, "classes")
        val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar, libJars = listOf(libJar))
        check(compileResult.success) { compileResult.log }

        val dexDir = File(projectDir, "dex")
        val dexResult = Dexer.dex(classesDir, dexDir, androidJar, libJars = listOf(libJar))

        assertTrue(dexResult.log, dexResult.success)
        val definedClasses = DexClassDefs.definedClasses(File(dexDir, "classes.dex"))
        assertTrue("Expected Main among $definedClasses", definedClasses.contains("LMain;"))
        assertTrue("Expected testlib.Lib among $definedClasses", definedClasses.contains("Ltestlib/Lib;"))
    }

    @Test
    fun dexWithoutTheLibJarSucceedsButDoesNotContainTheLibrarysClasses() {
        // Documents the exact asymmetry that makes DexClassDefs necessary: unlike ECJ compilation,
        // D8 dexing does not require referenced-but-absent classes to resolve, so "dex succeeds"
        // alone is not proof the library was actually merged in -- only inspecting the dex's own
        // class_def table (as the test above does) proves that.
        val libJar = TestLibJarBuilder.build(tempFolder.newFolder("lib-work"), androidJar)
        val projectDir = appProjectReferencingLib()
        val classesDir = File(projectDir, "classes")
        val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar, libJars = listOf(libJar))
        check(compileResult.success) { compileResult.log }

        val dexDir = File(projectDir, "dex")
        val dexResult = Dexer.dex(classesDir, dexDir, androidJar)

        assertTrue(dexResult.log, dexResult.success)
        val definedClasses = DexClassDefs.definedClasses(File(dexDir, "classes.dex"))
        assertTrue(definedClasses.contains("LMain;"))
        assertFalse(definedClasses.contains("Ltestlib/Lib;"))
    }
}
