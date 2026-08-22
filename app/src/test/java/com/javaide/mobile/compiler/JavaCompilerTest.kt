package com.javaide.mobile.compiler

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JavaCompilerTest {

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
    fun `compiles valid Hello World source successfully`() {
        val projectDir = tempFolder.newFolder("project")
        val srcDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(srcDir, "Main.java").writeText(
            """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            """.trimIndent()
        )
        val outputDir = tempFolder.newFolder("classes")

        val result = JavaCompiler.compile(projectDir, outputDir, androidJar)

        assertTrue("Expected compilation success but got: ${result.log}", result.success)
        assertTrue("Expected .class file output", outputDir.walkTopDown().any { it.extension == "class" })
    }

    @Test
    fun `returns failure for source with syntax error`() {
        val projectDir = tempFolder.newFolder("project")
        val srcDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(srcDir, "Main.java").writeText("public class Main { this is not valid java }")
        val outputDir = tempFolder.newFolder("classes")

        val result = JavaCompiler.compile(projectDir, outputDir, androidJar)

        assertFalse("Expected compilation failure for invalid source", result.success)
    }

    @Test
    fun `returns failure when no java files present`() {
        val projectDir = tempFolder.newFolder("project")
        File(projectDir, "src/main/java").mkdirs()
        val outputDir = tempFolder.newFolder("classes")

        val result = JavaCompiler.compile(projectDir, outputDir, androidJar)

        assertFalse(result.success)
        assertTrue(result.log.contains("No .java files"))
    }

    @Test
    fun `does not throw when android jar is missing`() {
        val projectDir = tempFolder.newFolder("project")
        val srcDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(srcDir, "Main.java").writeText("public class Main {}")
        val outputDir = tempFolder.newFolder("classes")
        val missingJar = File(tempFolder.root, "nonexistent.jar")

        val result = JavaCompiler.compile(projectDir, outputDir, missingJar)

        // Should not throw — either reports failure or succeeds without Android API resolution
        assertTrue(result.log.isNotBlank())
    }
}
