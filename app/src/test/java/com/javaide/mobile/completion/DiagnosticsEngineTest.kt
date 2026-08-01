package com.javaide.mobile.completion

import com.javaide.mobile.compiler.JavaCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DiagnosticsEngineTest {

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
    fun cleanCodeHasNoIssues() {
        val source = """
            |public class Clean {
            |    public static void main(String[] args) {
            |        System.out.println("hi");
            |    }
            |}
        """.trimMargin()

        val issues = DiagnosticsEngine.analyze(androidJar, source, "Clean.java")

        assertTrue(issues.isEmpty())
    }

    @Test
    fun syntaxErrorIsReportedAsAnError() {
        val source = """
            |public class Broken {
            |    public static void main(String[] args) {
            |        int x = 5
            |    }
            |}
        """.trimMargin()

        val issues = DiagnosticsEngine.analyze(androidJar, source, "Broken.java")

        assertEquals(1, issues.size)
        assertEquals(DiagnosticSeverity.ERROR, issues[0].severity)
        assertTrue(issues[0].unresolvedTypeName == null)
    }

    @Test
    fun unresolvedTypeReportsTheSimpleNameForEachOccurrence() {
        val source = """
            |public class Foo {
            |    public void bar() {
            |        ArrayList list = new ArrayList();
            |        Helper h = new Helper();
            |    }
            |}
        """.trimMargin()

        val issues = DiagnosticsEngine.analyze(androidJar, source, "Foo.java")

        // One issue for the declared type and one for the constructor call, per unresolved name.
        assertEquals(4, issues.size)
        assertTrue(issues.all { it.severity == DiagnosticSeverity.ERROR })
        assertEquals(setOf("ArrayList", "Helper"), issues.mapNotNull { it.unresolvedTypeName }.toSet())
        assertEquals(2, issues.count { it.unresolvedTypeName == "ArrayList" })
        assertEquals(2, issues.count { it.unresolvedTypeName == "Helper" })
    }

    @Test
    fun crossFileClasspathResolvesTypesCompiledFromAnotherFile() {
        val projectDir = tempFolder.newFolder("CrossFileProj")
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "Helper.java").writeText(
            "public class Helper {\n" +
                "    public int computeScore(String s) { return s.length(); }\n" +
                "}\n"
        )

        val classesDir = File(projectDir, "classes")
        val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar)
        check(compileResult.success) { compileResult.log }

        val mainSource = """
            |public class Main {
            |    public void run() {
            |        Helper h = new Helper();
            |    }
            |}
        """.trimMargin()

        val withoutClasspath = DiagnosticsEngine.analyze(androidJar, mainSource, "Main.java", projectClassesDir = null)
        val withClasspath = DiagnosticsEngine.analyze(androidJar, mainSource, "Main.java", projectClassesDir = classesDir)

        assertTrue(withoutClasspath.any { it.unresolvedTypeName == "Helper" })
        assertTrue(withClasspath.none { it.unresolvedTypeName == "Helper" })
    }
}
