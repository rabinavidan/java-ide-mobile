package com.javaide.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PackageRenamerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun androidProject(packageName: String): File {
        val projectDir = tempFolder.newFolder("Proj-${packageName.hashCode()}")
        val javaDir = File(projectDir, "src/main/java/${packageName.replace('.', '/')}").apply { mkdirs() }
        File(javaDir, "MainActivity.java").writeText(
            "package $packageName;\n\npublic class MainActivity {\n}\n"
        )
        File(projectDir, "src/main/AndroidManifest.xml").writeText(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    package=\"$packageName\">\n" +
                "</manifest>\n"
        )
        return projectDir
    }

    @Test
    fun isValidPackageNameRequiresAtLeastOneDot() {
        assertTrue(PackageRenamer.isValidPackageName("com.example"))
        assertTrue(PackageRenamer.isValidPackageName("com.example.app"))
        assertFalse(PackageRenamer.isValidPackageName("example"))
        assertFalse(PackageRenamer.isValidPackageName("com.1example"))
        assertFalse(PackageRenamer.isValidPackageName("com..example"))
        assertFalse(PackageRenamer.isValidPackageName(""))
    }

    @Test
    fun currentPackageNameInfersFromSourceFileLocation() {
        val projectDir = androidProject("com.example.myapp")

        assertEquals("com.example.myapp", PackageRenamer.currentPackageName(projectDir))
    }

    @Test
    fun currentPackageNameReturnsNullWhenNoSourceExists() {
        val projectDir = tempFolder.newFolder("Empty")
        File(projectDir, "src/main/java").mkdirs()

        assertNull(PackageRenamer.currentPackageName(projectDir))
    }

    @Test
    fun renameMovesSourceTreeAndRewritesPackageDeclaration() {
        val projectDir = androidProject("com.example.myapp")

        PackageRenamer.rename(projectDir, "com.example.myapp", "com.example.renamed")

        val oldFile = File(projectDir, "src/main/java/com/example/myapp/MainActivity.java")
        val newFile = File(projectDir, "src/main/java/com/example/renamed/MainActivity.java")
        assertFalse(oldFile.exists())
        assertTrue(newFile.isFile)
        assertTrue(newFile.readText().contains("package com.example.renamed;"))
    }

    @Test
    fun renameUpdatesManifestPackageAttribute() {
        val projectDir = androidProject("com.example.myapp")

        PackageRenamer.rename(projectDir, "com.example.myapp", "com.example.renamed")

        val manifestText = File(projectDir, "src/main/AndroidManifest.xml").readText()
        assertTrue(manifestText.contains("package=\"com.example.renamed\""))
        assertFalse(manifestText.contains("com.example.myapp"))
    }

    @Test
    fun renameSkipsManifestUpdateWhenNoneExists() {
        val projectDir = tempFolder.newFolder("ConsoleProj")
        val javaDir = File(projectDir, "src/main/java/com/example/console").apply { mkdirs() }
        File(javaDir, "Main.java").writeText("package com.example.console;\n\npublic class Main {\n}\n")

        PackageRenamer.rename(projectDir, "com.example.console", "org.other.name")

        val newFile = File(projectDir, "src/main/java/org/other/name/Main.java")
        assertTrue(newFile.isFile)
        assertTrue(newFile.readText().contains("package org.other.name;"))
    }

    @Test
    fun renameAcrossDifferentTopLevelPrefixesCleansUpEmptyAncestors() {
        val projectDir = androidProject("com.example.myapp")

        PackageRenamer.rename(projectDir, "com.example.myapp", "org.other.thing")

        val javaRoot = File(projectDir, "src/main/java")
        assertFalse(File(javaRoot, "com").exists())
        assertTrue(File(javaRoot, "org/other/thing/MainActivity.java").isFile)
    }

    @Test
    fun renameHandlesNestedSubPackages() {
        val projectDir = androidProject("com.example.myapp")
        val subDir = File(projectDir, "src/main/java/com/example/myapp/util").apply { mkdirs() }
        File(subDir, "Helper.java").writeText("package com.example.myapp.util;\n\npublic class Helper {\n}\n")

        PackageRenamer.rename(projectDir, "com.example.myapp", "com.example.renamed")

        val movedHelper = File(projectDir, "src/main/java/com/example/renamed/util/Helper.java")
        assertTrue(movedHelper.isFile)
        assertTrue(movedHelper.readText().contains("package com.example.renamed.util;"))
    }

    @Test(expected = IllegalStateException::class)
    fun renameRejectsInvalidNewPackageName() {
        val projectDir = androidProject("com.example.myapp")

        PackageRenamer.rename(projectDir, "com.example.myapp", "not valid")
    }

    @Test(expected = IllegalStateException::class)
    fun renameFailsWhenOldPackageDirectoryDoesNotExist() {
        val projectDir = androidProject("com.example.myapp")

        PackageRenamer.rename(projectDir, "com.does.not.exist", "com.example.renamed")
    }
}
