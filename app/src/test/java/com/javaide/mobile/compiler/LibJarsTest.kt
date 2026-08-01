package com.javaide.mobile.compiler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LibJarsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun jarsInReturnsEmptyListWhenLibsFolderAbsent() {
        val projectDir = tempFolder.newFolder("no-libs")
        assertTrue(LibJars.jarsIn(projectDir).isEmpty())
    }

    @Test
    fun jarsInReturnsEmptyListWhenLibsFolderEmpty() {
        val projectDir = tempFolder.newFolder("empty-libs")
        File(projectDir, "libs").mkdirs()
        assertTrue(LibJars.jarsIn(projectDir).isEmpty())
    }

    @Test
    fun jarsInFindsJarsSortedByName() {
        val projectDir = tempFolder.newFolder("with-libs")
        val libsDir = File(projectDir, "libs").apply { mkdirs() }
        File(libsDir, "zeta.jar").writeText("dummy")
        File(libsDir, "alpha.jar").writeText("dummy")

        val jars = LibJars.jarsIn(projectDir)

        assertEquals(listOf("alpha.jar", "zeta.jar"), jars.map { it.name })
    }

    @Test
    fun jarsInIgnoresNonJarFilesAndSubdirectories() {
        val projectDir = tempFolder.newFolder("mixed-libs")
        val libsDir = File(projectDir, "libs").apply { mkdirs() }
        File(libsDir, "real.jar").writeText("dummy")
        File(libsDir, "notes.txt").writeText("dummy")
        File(libsDir, "subdir").mkdirs()

        val jars = LibJars.jarsIn(projectDir)

        assertEquals(listOf("real.jar"), jars.map { it.name })
    }
}
