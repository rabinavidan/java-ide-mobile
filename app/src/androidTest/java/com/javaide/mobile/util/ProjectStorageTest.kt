package com.javaide.mobile.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.javaide.mobile.model.ProjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Verifies that projects are stored in getExternalFilesDir, not filesDir.
 * External files dir survives app updates/reinstalls (no WRITE_EXTERNAL_STORAGE needed on API 26+).
 */
@RunWith(AndroidJUnit4::class)
class ProjectStorageTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun projectsRootIsInExternalFilesDir() {
        val root = ProjectStorage.projectsRoot(ctx)
        val externalBase = ctx.getExternalFilesDir(null)

        // If external storage is available, root must be under it (not under filesDir)
        if (externalBase != null) {
            assertTrue(
                "Expected projects root under external files dir but was: ${root.absolutePath}",
                root.canonicalPath.startsWith(externalBase.canonicalPath)
            )
            assertFalse(
                "Projects root must NOT be under internal filesDir",
                root.canonicalPath.startsWith(ctx.filesDir.canonicalPath)
            )
        }
    }

    @Test
    fun createdProjectPersistsUnderExternalRoot() {
        val name = "StorageTest_${System.currentTimeMillis()}"
        try {
            val project = ProjectStorage.createProject(ctx, name, ProjectType.JAVA_CONSOLE)

            assertTrue("Project dir should exist", project.exists())
            val externalBase = ctx.getExternalFilesDir(null)
            if (externalBase != null) {
                assertTrue(
                    "Created project should live under external files dir",
                    project.canonicalPath.startsWith(externalBase.canonicalPath)
                )
            }

            val listed = ProjectStorage.listProjects(ctx).map { it.name }
            assertTrue("Created project should appear in list", name in listed)
        } finally {
            val dir = File(ProjectStorage.projectsRoot(ctx), name)
            if (dir.exists()) dir.deleteRecursively()
        }
    }

    @Test
    fun projectsRootDirectoryIsCreatedAutomatically() {
        val root = ProjectStorage.projectsRoot(ctx)
        assertTrue("projectsRoot must exist after call", root.exists())
        assertTrue("projectsRoot must be a directory", root.isDirectory)
    }

    @Test
    fun listProjectsReturnsOnlyDirectories() {
        val root = ProjectStorage.projectsRoot(ctx)
        // Create a stray file that should be excluded
        val strayFile = File(root, "not_a_project.txt").apply { createNewFile() }
        try {
            val projects = ProjectStorage.listProjects(ctx)
            assertFalse("listProjects must not return plain files", projects.any { it.name == "not_a_project.txt" })
        } finally {
            strayFile.delete()
        }
    }

    @Test
    fun listProjectsIsSortedAlphabetically() {
        val names = listOf("Zebra_${System.currentTimeMillis()}", "Apple_${System.currentTimeMillis()}")
        val created = mutableListOf<File>()
        try {
            names.forEach { name ->
                created += ProjectStorage.createProject(ctx, name, ProjectType.JAVA_CONSOLE)
            }
            val listed = ProjectStorage.listProjects(ctx).map { it.name }
            val ourProjects = listed.filter { n -> names.any { n == it } }
            assertEquals(names.sortedBy { it.lowercase() }, ourProjects)
        } finally {
            created.forEach { it.deleteRecursively() }
        }
    }
}
