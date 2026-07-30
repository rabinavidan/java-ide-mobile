package com.javaide.mobile.compiler

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object ManifestUtils {

    /** Reads the `package` attribute off a project's AndroidManifest.xml root element. */
    fun readPackageName(manifestFile: File): String {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)
        return doc.documentElement.getAttribute("package")
    }
}
