package com.javaide.mobile.compiler

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ManifestUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun readsPackageAttributeOffRootElement() {
        val manifestFile = tempFolder.newFile("AndroidManifest.xml")
        manifestFile.writeText(
            """
            |<?xml version="1.0" encoding="utf-8"?>
            |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
            |    package="com.example.foo">
            |
            |    <application android:label="Foo">
            |        <activity android:name=".MainActivity" />
            |    </application>
            |</manifest>
            """.trimMargin()
        )

        assertEquals("com.example.foo", ManifestUtils.readPackageName(manifestFile))
    }
}
