package com.javaide.mobile.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Only covers the functions that don't need an Android Context (isValidProjectName,
 * deleteProject). createProject/renameProject/listProjects/projectExists all take a Context to
 * locate the app's private storage, which needs Robolectric or a real device to construct --
 * out of scope for these plain-JVM unit tests (see the "logic-layer tests only" scoping decision).
 */
class ProjectStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun validProjectNamesAccepted() {
        assertTrue(ProjectStorage.isValidProjectName("MyProject"))
        assertTrue(ProjectStorage.isValidProjectName("my_project_2"))
        assertTrue(ProjectStorage.isValidProjectName("A"))
    }

    @Test
    fun invalidProjectNamesRejected() {
        assertFalse(ProjectStorage.isValidProjectName(""))
        assertFalse(ProjectStorage.isValidProjectName("2StartsWithDigit"))
        assertFalse(ProjectStorage.isValidProjectName("has space"))
        assertFalse(ProjectStorage.isValidProjectName("has-dash"))
        assertFalse(ProjectStorage.isValidProjectName("has/slash"))
    }

    @Test
    fun deleteProjectRemovesDirectoryAndContents() {
        val project = tempFolder.newFolder("ToDelete")
        File(project, "src/main/java").mkdirs()
        File(project, "src/main/java/Main.java").writeText("class Main {}")

        ProjectStorage.deleteProject(project)

        assertFalse(project.exists())
    }

    @Test
    fun deleteProjectIsANoOpIfDirectoryDoesNotExist() {
        // File.deleteRecursively() treats "already doesn't exist" as success, not failure --
        // deleteProject() doesn't throw here, it just has nothing to do.
        val project = File(tempFolder.root, "DoesNotExist")

        ProjectStorage.deleteProject(project)

        assertFalse(project.exists())
    }
}
