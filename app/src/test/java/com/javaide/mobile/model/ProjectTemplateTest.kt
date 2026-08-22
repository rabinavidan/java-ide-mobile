package com.javaide.mobile.model

import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectTemplateTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `Java console project creates Main java with main method`() {
        val dir = tempFolder.newFolder("project")

        ProjectTemplate.create(dir, "TestProject", "com.example", ProjectType.JAVA_CONSOLE)

        val main = File(dir, "src/main/java/com/example/Main.java")
        assertTrue("Main.java should exist", main.exists())
        val src = main.readText()
        assertTrue(src.contains("package com.example"))
        assertTrue(src.contains("public static void main"))
        assertTrue(src.contains("System.out.println"))
    }

    @Test
    fun `Java console project only creates source file, no manifest`() {
        val dir = tempFolder.newFolder("project")

        ProjectTemplate.create(dir, "TestProject", "com.example", ProjectType.JAVA_CONSOLE)

        assertTrue(File(dir, "src/main/java/com/example/Main.java").exists())
        assertTrue("Console project must not have a manifest", !File(dir, "src/main/AndroidManifest.xml").exists())
    }

    @Test
    fun `Android app project creates manifest, activity, layout and strings`() {
        val dir = tempFolder.newFolder("project")

        ProjectTemplate.create(dir, "MyApp", "com.example", ProjectType.ANDROID_APP)

        assertTrue(File(dir, "src/main/AndroidManifest.xml").exists())
        assertTrue(File(dir, "src/main/java/com/example/MainActivity.java").exists())
        assertTrue(File(dir, "src/main/res/layout/activity_main.xml").exists())
        assertTrue(File(dir, "src/main/res/values/strings.xml").exists())
    }

    @Test
    fun `Android manifest contains the package name`() {
        val dir = tempFolder.newFolder("project")

        ProjectTemplate.create(dir, "MyApp", "com.test.pkg", ProjectType.ANDROID_APP)

        val manifest = File(dir, "src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("com.test.pkg"))
    }

    @Test
    fun `Android strings xml contains project name`() {
        val dir = tempFolder.newFolder("project")

        ProjectTemplate.create(dir, "CoolApp", "com.example", ProjectType.ANDROID_APP)

        val strings = File(dir, "src/main/res/values/strings.xml").readText()
        assertTrue(strings.contains("CoolApp"))
    }

    @Test
    fun `nested package directories are created correctly`() {
        val dir = tempFolder.newFolder("project")

        ProjectTemplate.create(dir, "Test", "com.foo.bar", ProjectType.JAVA_CONSOLE)

        assertTrue(File(dir, "src/main/java/com/foo/bar/Main.java").exists())
    }
}
