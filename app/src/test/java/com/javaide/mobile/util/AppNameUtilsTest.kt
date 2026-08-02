package com.javaide.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppNameUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun stringsFile(appName: String): File {
        val file = tempFolder.newFile("strings.xml")
        file.writeText(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <string name=\"app_name\">$appName</string>\n" +
                "    <string name=\"other\">Unrelated</string>\n" +
                "</resources>\n"
        )
        return file
    }

    @Test
    fun readReturnsTheAppNameValue() {
        val file = stringsFile("MyProject")

        assertEquals("MyProject", AppNameUtils.read(file))
    }

    @Test
    fun readReturnsNullWhenFileDoesNotExist() {
        assertNull(AppNameUtils.read(File(tempFolder.root, "missing.xml")))
    }

    @Test
    fun writeUpdatesOnlyTheAppNameElement() {
        val file = stringsFile("Old Name")

        AppNameUtils.write(file, "New Name")

        assertEquals("New Name", AppNameUtils.read(file))
        val doc = file.readText()
        assertEquals(1, Regex("Unrelated").findAll(doc).count())
    }

    @Test
    fun writeEscapesXmlSpecialCharacters() {
        val file = stringsFile("Old Name")

        AppNameUtils.write(file, "Tom & Jerry's App")

        val text = file.readText()
        assert(text.contains("Tom &amp; Jerry's App"))
        assertEquals("Tom & Jerry's App", AppNameUtils.read(file))
    }
}
