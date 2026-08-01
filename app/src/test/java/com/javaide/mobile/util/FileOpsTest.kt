package com.javaide.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileOpsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun validFileNamesAccepted() {
        assertTrue(FileOps.isValidFileName("Helper.java"))
        assertTrue(FileOps.isValidFileName("notes.txt"))
    }

    @Test
    fun invalidFileNamesRejected() {
        assertFalse(FileOps.isValidFileName(""))
        assertFalse(FileOps.isValidFileName("   "))
        assertFalse(FileOps.isValidFileName("a/b.java"))
        assertFalse(FileOps.isValidFileName("a\\b.java"))
        assertFalse(FileOps.isValidFileName("."))
        assertFalse(FileOps.isValidFileName(".."))
    }

    @Test
    fun createFileSeedsPackageAwareSkeletonInNestedPackageDir() {
        val projectDir = tempFolder.newFolder("Proj")
        val packageDir = File(projectDir, "src/main/java/com/example/proj").apply { mkdirs() }

        val created = FileOps.createFile(projectDir, packageDir, "Helper.java")

        assertEquals("Helper.java", created.name)
        val text = created.readText()
        assertTrue(text.contains("package com.example.proj;"))
        assertTrue(text.contains("public class Helper {"))
    }

    @Test
    fun createFileDirectlyUnderJavaRootHasNoPackageLine() {
        val projectDir = tempFolder.newFolder("Proj2")
        val javaRoot = File(projectDir, "src/main/java").apply { mkdirs() }

        val created = FileOps.createFile(projectDir, javaRoot, "Solo.java")

        val text = created.readText()
        assertFalse(text.contains("package"))
        assertTrue(text.contains("public class Solo {"))
    }

    @Test
    fun createFileOutsideJavaRootHasNoPackageLineEvenForJavaExtension() {
        val projectDir = tempFolder.newFolder("Proj3")
        val otherDir = File(projectDir, "src/main/res/values").apply { mkdirs() }

        val created = FileOps.createFile(projectDir, otherDir, "Weird.java")

        assertFalse(created.readText().contains("package"))
    }

    @Test
    fun createFileInLookalikeJavaDirHasNoPackageLine() {
        // Regression check for a real bug caught during review: a bare String.startsWith(...)
        // on the java-root path would have wrongly treated "javaExtra" as being under "java".
        val projectDir = tempFolder.newFolder("Proj3b")
        val lookalikeDir = File(projectDir, "src/main/javaExtra").apply { mkdirs() }

        val created = FileOps.createFile(projectDir, lookalikeDir, "Weird.java")

        assertFalse(created.readText().contains("package"))
    }

    @Test
    fun createFileWithNonJavaNameCreatesEmptyFile() {
        val projectDir = tempFolder.newFolder("Proj4")

        val created = FileOps.createFile(projectDir, projectDir, "notes.txt")

        assertEquals("", created.readText())
    }

    @Test(expected = IllegalStateException::class)
    fun createFileThrowsWhenTargetAlreadyExists() {
        val projectDir = tempFolder.newFolder("Proj5")
        File(projectDir, "Dup.txt").writeText("x")

        FileOps.createFile(projectDir, projectDir, "Dup.txt")
    }

    @Test
    fun createFolderMakesANewDirectory() {
        val parent = tempFolder.newFolder("Parent")

        val created = FileOps.createFolder(parent, "child")

        assertTrue(created.isDirectory)
        assertTrue(File(parent, "child").isDirectory)
    }

    @Test(expected = IllegalStateException::class)
    fun createFolderThrowsWhenTargetAlreadyExists() {
        val parent = tempFolder.newFolder("ParentDup")
        File(parent, "child").mkdirs()

        FileOps.createFolder(parent, "child")
    }

    @Test
    fun renameEntryRenamesAFileInPlace() {
        val parent = tempFolder.newFolder("RenameDir")
        val original = File(parent, "Old.java").apply { writeText("content") }

        val renamed = FileOps.renameEntry(original, "New.java")

        assertEquals("New.java", renamed.name)
        assertEquals("content", renamed.readText())
        assertFalse(original.exists())
    }

    @Test(expected = IllegalStateException::class)
    fun renameEntryThrowsWhenTargetNameAlreadyExists() {
        val parent = tempFolder.newFolder("RenameDirDup")
        val original = File(parent, "Old.java").apply { writeText("x") }
        File(parent, "New.java").writeText("y")

        FileOps.renameEntry(original, "New.java")
    }

    @Test
    fun deleteEntryRemovesASingleFile() {
        val parent = tempFolder.newFolder("DeleteDir")
        val file = File(parent, "Gone.java").apply { writeText("x") }

        FileOps.deleteEntry(file)

        assertFalse(file.exists())
    }

    @Test
    fun deleteEntryRecursivelyRemovesAFolderAndItsContents() {
        val parent = tempFolder.newFolder("DeleteFolderDir")
        val sub = File(parent, "nested").apply { mkdirs() }
        File(sub, "Inside.java").writeText("x")

        FileOps.deleteEntry(sub)

        assertFalse(sub.exists())
    }
}
