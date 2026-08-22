package com.javaide.mobile.completion

import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.compiler.TestLibJarBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SemanticCompletionEngineTest {

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
    fun dotCompletionResolvesRealMembersOfACrossFileType() {
        val projectDir = tempFolder.newFolder("DotCompletionProj")
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "Helper.java").writeText(
            "public class Helper {\n" +
                "    public int computeScore(String s) { return s.length(); }\n" +
                "}\n"
        )
        val classesDir = File(projectDir, "classes")
        val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar)
        check(compileResult.success) { compileResult.log }

        val source = """
            |public class Foo {
            |    void bar() {
            |        Helper h = new Helper();
            |        h.comp
            |    }
            |}
        """.trimMargin()
        // Cursor right after "comp", mid-typing "h.comp" -- there's no earlier "comp" in this
        // source, but lastIndexOf mirrors how a real caret position is computed either way.
        val cursor = source.lastIndexOf("comp") + "comp".length

        val result = SemanticCompletionEngine.complete(androidJar, source, cursor, "Foo.java", classesDir)

        assertEquals(4, result.prefixLength)
        val methodNames = result.suggestions.filter { it.kind == SemanticKind.METHOD }.map { it.name }
        assertTrue(methodNames.contains("computeScore"))
        val computeScore = result.suggestions.first { it.name == "computeScore" }
        assertEquals(SemanticKind.METHOD, computeScore.kind)
        assertEquals("int", computeScore.detail)
    }

    @Test
    fun scopeCompletionFindsALocalVariableByPrefix() {
        val source = """
            |public class Foo {
            |    void bar() {
            |        int myLocalVariable = 5;
            |        myLo
            |    }
            |}
        """.trimMargin()
        // lastIndexOf, not indexOf: "myLocalVariable"'s own declaration also starts with "myLo",
        // so a plain indexOf would find the declaration site instead of the completion trigger.
        val cursor = source.lastIndexOf("myLo") + "myLo".length

        val result = SemanticCompletionEngine.complete(androidJar, source, cursor, "Foo.java")

        assertEquals(4, result.prefixLength)
        val local = result.suggestions.first { it.name == "myLocalVariable" }
        assertEquals(SemanticKind.LOCAL, local.kind)
        assertEquals("int", local.detail)
    }

    @Test
    fun emptyPrefixInScopeReturnsNoCrash() {
        val source = """
            |public class Foo {
            |    void bar() {
            |        int x = 5;
            |
            |    }
            |}
        """.trimMargin()
        val cursor = source.indexOf("int x = 5;") + "int x = 5;".length + 1

        val result = SemanticCompletionEngine.complete(androidJar, source, cursor, "Foo.java")

        assertEquals(0, result.prefixLength)
    }

    @Test
    fun dotCompletionResolvesMembersOfALibJarType() {
        val libJar = TestLibJarBuilder.build(tempFolder.newFolder("lib-work"), androidJar)

        val source = """
            |import testlib.Lib;
            |public class Foo {
            |    void bar() {
            |        Lib l = new Lib();
            |        l.va
            |    }
            |}
        """.trimMargin()
        val cursor = source.lastIndexOf("va") + "va".length

        val result = SemanticCompletionEngine.complete(
            androidJar, source, cursor, "Foo.java", libJars = listOf(libJar)
        )

        assertEquals(2, result.prefixLength)
        val value = result.suggestions.first { it.name == "value" }
        assertEquals(SemanticKind.FIELD, value.kind)
        assertEquals("int", value.detail)
    }
}
