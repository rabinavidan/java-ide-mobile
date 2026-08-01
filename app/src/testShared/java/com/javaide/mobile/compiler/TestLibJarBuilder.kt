package com.javaide.mobile.compiler

import java.io.File
import java.io.FileInputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Builds a tiny real .jar (compiled via the same JavaCompiler used in production) standing in
 * for a third-party dependency, so tests can prove libJars actually reach the classpath instead
 * of just reading the wiring code.
 *
 * Takes a plain [workDir] (rather than JUnit's TemporaryFolder rule) since this lives in
 * testShared, which -- unlike src/test -- is compiled as part of the "main" source set (see
 * app/build.gradle.kts) and so can't depend on JUnit.
 */
object TestLibJarBuilder {

    fun build(workDir: File, androidJar: File): File {
        val libProjectDir = File(workDir, "test-lib-src").apply { mkdirs() }
        val javaDir = File(libProjectDir, "src/main/java/testlib").apply { mkdirs() }
        File(javaDir, "Lib.java").writeText(
            """
            package testlib;
            public class Lib {
                public static String greet() { return "hi from lib"; }
                public int value = 42;
            }
            """.trimIndent()
        )

        val classesDir = File(libProjectDir, "classes")
        val compileResult = JavaCompiler.compile(libProjectDir, classesDir, androidJar)
        check(compileResult.success) { "Failed to build test library jar: ${compileResult.log}" }

        val jarDir = File(workDir, "test-lib-jar").apply { mkdirs() }
        val jarFile = File(jarDir, "testlib.jar")
        JarOutputStream(jarFile.outputStream()).use { jar ->
            val classFile = File(classesDir, "testlib/Lib.class")
            jar.putNextEntry(JarEntry("testlib/Lib.class"))
            FileInputStream(classFile).use { it.copyTo(jar) }
            jar.closeEntry()
        }
        return jarFile
    }
}
