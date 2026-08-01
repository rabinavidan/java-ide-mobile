package com.javaide.mobile.completion

import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.compiler.TestLibJarBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DefinitionFinderTest {

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
    fun sameFileLocalVariableResolvesToItsDeclaration() {
        val source = """
            |public class Foo {
            |    void bar() {
            |        int localVar = 5;
            |        int lv = localVar;
            |    }
            |}
        """.trimMargin()
        val cursor = source.lastIndexOf("localVar") + 4

        val target = DefinitionFinder.find(androidJar, source, cursor, "Foo.java")

        assertTrue(target is DefinitionTarget.SameFile)
        val offset = (target as DefinitionTarget.SameFile).offset
        assertEquals("localVar", source.substring(offset, offset + "localVar".length))
    }

    @Test
    fun sameFileFieldResolvesToItsDeclaration() {
        val source = """
            |public class Foo {
            |    int myField = 1;
            |    void bar() {
            |        int f = myField;
            |    }
            |}
        """.trimMargin()
        val cursor = source.lastIndexOf("myField") + 4

        val target = DefinitionFinder.find(androidJar, source, cursor, "Foo.java")

        assertTrue(target is DefinitionTarget.SameFile)
        val offset = (target as DefinitionTarget.SameFile).offset
        assertEquals("myField", source.substring(offset, offset + "myField".length))
        assertTrue(offset < source.lastIndexOf("myField"))
    }

    @Test
    fun sameFileMethodCallResolvesToItsDeclaration() {
        val source = """
            |public class Foo {
            |    int ownMethod() { return 2; }
            |    void bar() {
            |        int r = ownMethod();
            |    }
            |}
        """.trimMargin()
        val cursor = source.lastIndexOf("ownMethod") + 4

        val target = DefinitionFinder.find(androidJar, source, cursor, "Foo.java")

        assertTrue(target is DefinitionTarget.SameFile)
        val offset = (target as DefinitionTarget.SameFile).offset
        assertEquals("ownMethod", source.substring(offset, offset + "ownMethod".length))
        assertTrue(offset < source.lastIndexOf("ownMethod"))
    }

    @Test
    fun crossFileTypeResolvesToOtherProjectFileBySimpleName() {
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

        val source = """
            |public class Main {
            |    void run() {
            |        Helper h = new Helper();
            |    }
            |}
        """.trimMargin()
        val cursor = source.lastIndexOf("new Helper") + 5

        val target = DefinitionFinder.find(androidJar, source, cursor, "Main.java", projectClassesDir = classesDir)

        assertEquals(DefinitionTarget.OtherProjectFile("Helper"), target)
    }

    @Test
    fun crossFileMethodCallAlsoResolvesToOtherProjectFile() {
        val projectDir = tempFolder.newFolder("CrossFileMethodProj")
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
            |public class Main {
            |    void run() {
            |        Helper h = new Helper();
            |        int r = h.computeScore("x");
            |    }
            |}
        """.trimMargin()
        val cursor = source.lastIndexOf("computeScore") + 4

        val target = DefinitionFinder.find(androidJar, source, cursor, "Main.java", projectClassesDir = classesDir)

        assertEquals(DefinitionTarget.OtherProjectFile("Helper"), target)
    }

    @Test
    fun libJarTypeHasNoProjectSourceToJumpTo() {
        val libJar = TestLibJarBuilder.build(tempFolder.newFolder("lib-work"), androidJar)

        val source = """
            |import testlib.Lib;
            |public class Main {
            |    void run() {
            |        Lib l = new Lib();
            |    }
            |}
        """.trimMargin()
        val cursor = source.lastIndexOf("new Lib") + 5

        val target = DefinitionFinder.find(androidJar, source, cursor, "Main.java", libJars = listOf(libJar))

        assertNull(target)
    }

    @Test
    fun cursorNotOnAnyReferenceReturnsNull() {
        val source = """
            |public class Foo {
            |    void bar() {
            |        int x = 5;
            |    }
            |}
        """.trimMargin()
        val cursor = source.indexOf("void bar")

        val target = DefinitionFinder.find(androidJar, source, cursor, "Foo.java")

        assertNull(target)
    }
}
