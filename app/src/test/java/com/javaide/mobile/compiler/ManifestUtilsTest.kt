package com.javaide.mobile.compiler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ManifestUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun manifestFile(extra: String = ""): File {
        val file = tempFolder.newFile("AndroidManifest.xml")
        file.writeText(
            """
            |<?xml version="1.0" encoding="utf-8"?>
            |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
            |    package="com.example.foo">
            |
            |    <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="34" />
            |
            |    <application android:label="Foo"$extra>
            |        <activity android:name=".MainActivity" />
            |    </application>
            |</manifest>
            """.trimMargin()
        )
        return file
    }

    @Test
    fun readsPackageAttributeOffRootElement() {
        val manifestFile = manifestFile()

        assertEquals("com.example.foo", ManifestUtils.readPackageName(manifestFile))
    }

    @Test
    fun readsMinAndTargetSdkVersions() {
        val manifestFile = manifestFile()

        assertEquals(26, ManifestUtils.readMinSdkVersion(manifestFile))
        assertEquals(34, ManifestUtils.readTargetSdkVersion(manifestFile))
    }

    @Test
    fun readsNullSdkVersionsWhenUsesSdkElementIsMissing() {
        val file = tempFolder.newFile("NoSdk.xml")
        file.writeText(
            """
            |<?xml version="1.0" encoding="utf-8"?>
            |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
            |    package="com.example.foo">
            |    <application android:label="Foo" />
            |</manifest>
            """.trimMargin()
        )

        assertNull(ManifestUtils.readMinSdkVersion(file))
        assertNull(ManifestUtils.readTargetSdkVersion(file))
    }

    @Test
    fun writeSdkVersionsUpdatesExistingUsesSdkElement() {
        val manifestFile = manifestFile()

        ManifestUtils.writeSdkVersions(manifestFile, 28, 35)

        assertEquals(28, ManifestUtils.readMinSdkVersion(manifestFile))
        assertEquals(35, ManifestUtils.readTargetSdkVersion(manifestFile))
        assertEquals("com.example.foo", ManifestUtils.readPackageName(manifestFile))
    }

    @Test
    fun writeIconSetsApplicationIconAttribute() {
        val manifestFile = manifestFile()

        ManifestUtils.writeIcon(manifestFile, "ic_launcher")

        val text = manifestFile.readText()
        assertTrue(text.contains("android:icon=\"@mipmap/ic_launcher\""))
    }

    @Test
    fun writeIconUpdatesAnExistingIconAttribute() {
        val manifestFile = manifestFile(extra = " android:icon=\"@mipmap/old_icon\"")

        ManifestUtils.writeIcon(manifestFile, "ic_launcher")

        val text = manifestFile.readText()
        assertTrue(text.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(!text.contains("old_icon"))
    }
}
