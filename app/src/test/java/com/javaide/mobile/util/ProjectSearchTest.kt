package com.javaide.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectSearchTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun findsMatchesAcrossMultipleFilesWithCorrectLineNumbers() {
        val projectDir = tempFolder.newFolder("Proj")
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "Foo.java").writeText("public class Foo {\n    void bar() {}\n}\n")
        File(javaDir, "Baz.java").writeText("public class Baz {\n    void bar() {}\n}\n")

        val results = ProjectSearch.search(projectDir, "bar", caseSensitive = false)

        assertEquals(2, results.size)
        val fooResult = results.first { it.file.name == "Foo.java" }
        assertEquals(1, fooResult.matches.size)
        assertEquals(2, fooResult.matches[0].lineNumber)
        assertEquals("void bar() {}", fooResult.matches[0].lineText)
    }

    @Test
    fun caseSensitiveToggleRestrictsMatches() {
        val projectDir = tempFolder.newFolder("Proj2")
        File(projectDir, "Foo.java").writeText("Hello World\nhello world\n")

        val caseInsensitive = ProjectSearch.search(projectDir, "hello", caseSensitive = false)
        val caseSensitive = ProjectSearch.search(projectDir, "hello", caseSensitive = true)

        assertEquals(2, caseInsensitive.single().matches.size)
        assertEquals(1, caseSensitive.single().matches.size)
        assertEquals("hello world", caseSensitive.single().matches[0].lineText)
    }

    @Test
    fun excludesBuildAndGitDirectories() {
        val projectDir = tempFolder.newFolder("Proj3")
        File(projectDir, "src").mkdirs()
        File(projectDir, "src/Real.java").writeText("target line\n")
        File(projectDir, "build").mkdirs()
        File(projectDir, "build/Generated.java").writeText("target line\n")
        File(projectDir, ".git").mkdirs()
        File(projectDir, ".git/Internal.txt").writeText("target line\n")

        val results = ProjectSearch.search(projectDir, "target", caseSensitive = false)

        assertEquals(1, results.size)
        assertEquals("Real.java", results.single().file.name)
    }

    @Test
    fun returnsNoResultsWhenNothingMatches() {
        val projectDir = tempFolder.newFolder("Proj4")
        File(projectDir, "Foo.java").writeText("nothing interesting here\n")

        val results = ProjectSearch.search(projectDir, "unmatched", caseSensitive = false)

        assertTrue(results.isEmpty())
    }

    @Test
    fun returnsNoResultsForEmptyQuery() {
        val projectDir = tempFolder.newFolder("Proj5")
        File(projectDir, "Foo.java").writeText("anything\n")

        val results = ProjectSearch.search(projectDir, "", caseSensitive = false)

        assertTrue(results.isEmpty())
    }

    @Test
    fun replaceAllReplacesEveryOccurrenceAcrossMatchingFiles() {
        val projectDir = tempFolder.newFolder("Proj6")
        val javaDir = File(projectDir, "src").apply { mkdirs() }
        val foo = File(javaDir, "Foo.java").apply { writeText("void bar() {}\nbar();\n") }
        val baz = File(javaDir, "Baz.java").apply { writeText("no match here\n") }

        val changedFiles = ProjectSearch.replaceAll(projectDir, "bar", "baz", caseSensitive = false)

        assertEquals(1, changedFiles)
        assertEquals("void baz() {}\nbaz();\n", foo.readText())
        assertEquals("no match here\n", baz.readText())
    }

    @Test
    fun replaceAllReplacesPartialIdentifierMatchesLikeSearchDoes() {
        val projectDir = tempFolder.newFolder("Proj7")
        val file = File(projectDir, "Foo.java").apply { writeText("NodeList list = new NodeList();\n") }

        ProjectSearch.replaceAll(projectDir, "Node", "Element", caseSensitive = false)

        assertEquals("ElementList list = new ElementList();\n", file.readText())
    }

    @Test
    fun replaceAllRespectsCaseSensitiveToggle() {
        val projectDir = tempFolder.newFolder("Proj8")
        val file = File(projectDir, "Foo.java").apply { writeText("Hello World\nhello world\n") }

        ProjectSearch.replaceAll(projectDir, "hello", "hi", caseSensitive = true)

        assertEquals("Hello World\nhi world\n", file.readText())
    }

    @Test
    fun replaceAllExcludesBuildAndGitDirectories() {
        val projectDir = tempFolder.newFolder("Proj9")
        File(projectDir, "src").mkdirs()
        File(projectDir, "src/Real.java").writeText("target line\n")
        File(projectDir, "build").mkdirs()
        val generated = File(projectDir, "build/Generated.java").apply { writeText("target line\n") }
        File(projectDir, ".git").mkdirs()
        val internal = File(projectDir, ".git/Internal.txt").apply { writeText("target line\n") }

        val changedFiles = ProjectSearch.replaceAll(projectDir, "target", "renamed", caseSensitive = false)

        assertEquals(1, changedFiles)
        assertEquals("target line\n", generated.readText())
        assertEquals("target line\n", internal.readText())
    }

    @Test
    fun replaceAllReturnsZeroForEmptyQuery() {
        val projectDir = tempFolder.newFolder("Proj10")
        File(projectDir, "Foo.java").writeText("anything\n")

        assertEquals(0, ProjectSearch.replaceAll(projectDir, "", "x", caseSensitive = false))
    }
}
