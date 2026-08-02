package com.javaide.mobile.util

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Reads/writes the app_name string resource, the display name shown for an installed app. */
object AppNameUtils {

    fun read(stringsFile: File): String? {
        if (!stringsFile.isFile) return null
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile)
        return appNameElement(doc)?.textContent
    }

    /** Uses DOM serialization (not text substitution) so XML-special characters in [newName] --
     * unlike a project's folder/package name, this is a free-form display string -- are escaped
     * correctly rather than needing to be handled by hand. */
    fun write(stringsFile: File, newName: String) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile)
        val element = appNameElement(doc) ?: error("No app_name string found in ${stringsFile.absolutePath}")
        element.textContent = newName

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.transform(DOMSource(doc), StreamResult(stringsFile))
    }

    private fun appNameElement(doc: org.w3c.dom.Document): Element? {
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .firstOrNull { it.getAttribute("name") == "app_name" }
    }
}
