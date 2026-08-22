package com.javaide.mobile.compiler

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DexerTest {

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
    fun `dexes compiled classes into classes dex`() {
        val classesDir = compileHelloWorld()
        val dexDir = tempFolder.newFolder("dex")

        val result = Dexer.dex(classesDir, dexDir, androidJar)

        assertTrue("Expected dex success but got: ${result.log}", result.success)
        assertTrue("Expected classes.dex", File(dexDir, "classes.dex").exists())
    }

    @Test
    fun `returns failure when no class files present`() {
        val emptyDir = tempFolder.newFolder("empty-classes")
        val dexDir = tempFolder.newFolder("dex")

        val result = Dexer.dex(emptyDir, dexDir, androidJar)

        assertFalse(result.success)
        assertTrue(result.log.contains("No .class files"))
    }

    @Test
    fun `dex output does not contain class files`() {
        val classesDir = compileHelloWorld()
        val dexDir = tempFolder.newFolder("dex")

        Dexer.dex(classesDir, dexDir, androidJar)

        val dexFiles = dexDir.walkTopDown().filter { it.isFile }.toList()
        assertTrue("Expected at least one dex output file", dexFiles.isNotEmpty())
        assertTrue("Output should be .dex files only", dexFiles.all { it.extension == "dex" })
    }

    private fun compileHelloWorld(): File {
        val projectDir = tempFolder.newFolder("project")
        val srcDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(srcDir, "Main.java").writeText(
            """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
            """.trimIndent()
        )
        val classesDir = tempFolder.newFolder("classes")
        val result = JavaCompiler.compile(projectDir, classesDir, androidJar)
        check(result.success) { "Prerequisite compile failed: ${result.log}" }
        return classesDir
    }
}
