package com.javaide.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ClassRenamerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun isClassDeclarationTrueWhenNameMatchesTopLevelType() {
        val file = tempFolder.newFile("Helper.java")
        file.writeText("public class Helper {\n}\n")

        assertTrue(ClassRenamer.isClassDeclaration(file, "Helper"))
    }

    @Test
    fun isClassDeclarationTrueForInterfaceAndEnum() {
        val iface = tempFolder.newFile("Marker.java")
        iface.writeText("public interface Marker {\n}\n")
        val enumFile = tempFolder.newFile("Color.java")
        enumFile.writeText("public enum Color { RED, GREEN }\n")

        assertTrue(ClassRenamer.isClassDeclaration(iface, "Marker"))
        assertTrue(ClassRenamer.isClassDeclaration(enumFile, "Color"))
    }

    @Test
    fun isClassDeclarationFalseWhenNameDoesNotMatch() {
        val file = tempFolder.newFile("Helper.java")
        file.writeText("public class SomethingElse {\n}\n")

        assertFalse(ClassRenamer.isClassDeclaration(file, "Helper"))
    }

    @Test
    fun isClassDeclarationFalseForNonJavaFiles() {
        val file = tempFolder.newFile("notes.txt")
        file.writeText("class Helper mentioned in passing")

        assertFalse(ClassRenamer.isClassDeclaration(file, "Helper"))
    }

    @Test
    fun renameAcrossProjectRewritesDeclarationUsagesAndImports() {
        val projectDir = tempFolder.newFolder("Proj")
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        val helperFile = File(javaDir, "Helper.java").apply {
            writeText("public class Helper {\n}\n")
        }
        val mainFile = File(javaDir, "Main.java").apply {
            writeText(
                "import Helper;\n\n" +
                    "public class Main {\n" +
                    "    void run() {\n" +
                    "        Helper h = new Helper();\n" +
                    "    }\n" +
                    "}\n"
            )
        }

        val changedFiles = ClassRenamer.renameAcrossProject(projectDir, "Helper", "Utils")

        assertEquals(2, changedFiles)
        assertTrue(helperFile.readText().contains("public class Utils {"))
        val mainText = mainFile.readText()
        assertTrue(mainText.contains("import Utils;"))
        assertTrue(mainText.contains("Utils h = new Utils();"))
        assertFalse(mainText.contains("Helper"))
    }

    @Test
    fun renameAcrossProjectLeavesUnrelatedFilesAlone() {
        val projectDir = tempFolder.newFolder("Proj2")
        val javaDir = File(projectDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "Helper.java").writeText("public class Helper {\n}\n")
        val unrelated = File(javaDir, "Unrelated.java").apply {
            writeText("public class Unrelated {\n    int x;\n}\n")
        }

        val changedFiles = ClassRenamer.renameAcrossProject(projectDir, "Helper", "Utils")

        assertEquals(1, changedFiles)
        assertEquals("public class Unrelated {\n    int x;\n}\n", unrelated.readText())
    }

    @Test
    fun renameAcrossProjectDoesNotMatchNamesThatOnlyShareAPrefix() {
        val projectDir = tempFolder.newFolder("Proj3")
        val file = File(projectDir, "Foo.java").apply {
            writeText("public class Foo {\n    int HelperUtility = 1;\n}\n")
        }

        val changedFiles = ClassRenamer.renameAcrossProject(projectDir, "Helper", "Utils")

        assertEquals(0, changedFiles)
        assertTrue(file.readText().contains("HelperUtility"))
    }

    @Test
    fun renameAcrossProjectSweepsMatchingTextInsideCommentsAndStrings() {
        // Documented limitation: this is a whole-word text substitution, not ECJ reference
        // resolution, so a comment or string literal that happens to contain the exact word is
        // rewritten too. Asserting this explicitly so the behavior stays intentional, not a
        // silent regression either direction.
        val projectDir = tempFolder.newFolder("Proj4")
        val file = File(projectDir, "Foo.java").apply {
            writeText(
                "public class Foo {\n" +
                    "    // mentions Helper in a comment\n" +
                    "    String s = \"Helper\";\n" +
                    "}\n"
            )
        }

        ClassRenamer.renameAcrossProject(projectDir, "Helper", "Utils")

        val text = file.readText()
        assertTrue(text.contains("// mentions Utils in a comment"))
        assertTrue(text.contains("String s = \"Utils\";"))
    }

    @Test
    fun renameAcrossProjectExcludesBuildAndGitDirectories() {
        val projectDir = tempFolder.newFolder("Proj5")
        File(projectDir, "src").mkdirs()
        File(projectDir, "src/Real.java").writeText("class Helper {}")
        File(projectDir, "build").mkdirs()
        val generated = File(projectDir, "build/Generated.java").apply { writeText("class Helper {}") }
        File(projectDir, ".git").mkdirs()
        val internal = File(projectDir, ".git/Internal.java").apply { writeText("class Helper {}") }

        val changedFiles = ClassRenamer.renameAcrossProject(projectDir, "Helper", "Utils")

        assertEquals(1, changedFiles)
        assertTrue(generated.readText().contains("Helper"))
        assertTrue(internal.readText().contains("Helper"))
    }
}
