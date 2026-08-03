package com.javaide.mobile.compiler

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object ManifestUtils {

    /** Reads the `package` attribute off a project's AndroidManifest.xml root element. */
    fun readPackageName(manifestFile: File): String {
        val doc = parse(manifestFile)
        return doc.documentElement.getAttribute("package")
    }

    fun readMinSdkVersion(manifestFile: File): Int? = readUsesSdkAttribute(manifestFile, "android:minSdkVersion")

    fun readTargetSdkVersion(manifestFile: File): Int? = readUsesSdkAttribute(manifestFile, "android:targetSdkVersion")

    /** Updates (or, if somehow missing, creates) the `<uses-sdk>` element's min/target versions. */
    fun writeSdkVersions(manifestFile: File, minSdkVersion: Int, targetSdkVersion: Int) {
        val doc = parse(manifestFile)
        val usesSdk = doc.getElementsByTagName("uses-sdk").item(0) as? Element
            ?: doc.createElement("uses-sdk").also { doc.documentElement.appendChild(it) }
        usesSdk.setAttribute("android:minSdkVersion", minSdkVersion.toString())
        usesSdk.setAttribute("android:targetSdkVersion", targetSdkVersion.toString())
        serialize(doc, manifestFile)
    }

    /** Sets the `<application>` element's `android:icon` to reference [mipmapName] (e.g. "ic_launcher"). */
    fun writeIcon(manifestFile: File, mipmapName: String) {
        val doc = parse(manifestFile)
        val application = doc.getElementsByTagName("application").item(0) as Element
        application.setAttribute("android:icon", "@mipmap/$mipmapName")
        serialize(doc, manifestFile)
    }

    private fun readUsesSdkAttribute(manifestFile: File, attributeName: String): Int? {
        val doc = parse(manifestFile)
        val usesSdk = doc.getElementsByTagName("uses-sdk").item(0) as? Element ?: return null
        return usesSdk.getAttribute(attributeName).toIntOrNull()
    }

    private fun parse(manifestFile: File) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)

    private fun serialize(doc: org.w3c.dom.Document, file: File) {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.transform(DOMSource(doc), StreamResult(file))
    }
}
