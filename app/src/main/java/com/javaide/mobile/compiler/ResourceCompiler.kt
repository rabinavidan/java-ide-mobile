package com.javaide.mobile.compiler

import com.reandroid.apk.ApkModule
import com.reandroid.apk.ApkModuleXmlEncoder
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

data class ResourceCompileResult(val success: Boolean, val log: String, val apkModule: ApkModule? = null)

/**
 * Compiles a project's AndroidManifest.xml and simple resources (strings, layouts, an optional
 * app icon) into a binary ApkModule using ARSCLib, and generates the matching R.java.
 *
 * This only supports what the generated project template (plus Project Settings' icon picker)
 * uses: a single unqualified res/values/strings.xml, res/layout/*.xml files, and at most one
 * non-density-qualified res/mipmap/ic_launcher.png. Styles, qualifiers, adaptive icons, etc.
 * are not handled yet.
 */
object ResourceCompiler {

    /** Conventional, fixed location/name Project Settings' icon picker (IconUtils) saves to --
     * there's only ever one app icon per project, so no id/name needs to be user-chosen. */
    const val ICON_RESOURCE_NAME = "ic_launcher"

    private const val STRING_ID_BASE = 0x7f010000
    private const val LAYOUT_ID_BASE = 0x7f020000
    private const val MIPMAP_ID_BASE = 0x7f030000

    private data class ResourceIds(
        val strings: Map<String, Int>,
        val layouts: Map<String, Int>,
        val mipmaps: Map<String, Int>
    )

    fun compile(projectDir: File, androidJar: File, generatedSourcesDir: File): ResourceCompileResult {
        val mainDir = File(projectDir, "src/main")
        val manifestFile = File(mainDir, "AndroidManifest.xml")
        if (!manifestFile.isFile) {
            return ResourceCompileResult(success = false, log = "AndroidManifest.xml not found at ${manifestFile.absolutePath}")
        }

        return try {
            val packageName = ManifestUtils.readPackageName(manifestFile)
            val stringsFile = File(mainDir, "res/values/strings.xml")
            val stringNames = readStringNames(stringsFile)
            val layoutFiles = File(mainDir, "res/layout").listFiles { f -> f.isFile && f.extension == "xml" }
                ?.sortedBy { it.name }.orEmpty()
            val iconFile = File(mainDir, "res/mipmap/$ICON_RESOURCE_NAME.png").takeIf { it.isFile }

            val ids = ResourceIds(
                strings = stringNames.withIndex().associate { (i, name) -> name to (STRING_ID_BASE + i) },
                layouts = layoutFiles.withIndex().associate { (i, f) -> f.nameWithoutExtension to (LAYOUT_ID_BASE + i) },
                mipmaps = if (iconFile != null) mapOf(ICON_RESOURCE_NAME to MIPMAP_ID_BASE) else emptyMap()
            )

            writeRJava(generatedSourcesDir, packageName, ids)

            val stagingDir = File(projectDir, "build/res-staging").apply {
                deleteRecursively()
                mkdirs()
            }
            manifestFile.copyTo(File(stagingDir, "AndroidManifest.xml"), overwrite = true)

            val resDir = File(stagingDir, "resources/app/res")
            val valuesDir = File(resDir, "values").apply { mkdirs() }
            if (stringsFile.isFile) {
                stringsFile.copyTo(File(valuesDir, "strings.xml"), overwrite = true)
            }
            writePublicXml(File(valuesDir, "public.xml"), ids)

            if (layoutFiles.isNotEmpty()) {
                val layoutDir = File(resDir, "layout").apply { mkdirs() }
                layoutFiles.forEach { it.copyTo(File(layoutDir, it.name), overwrite = true) }
            }

            if (iconFile != null) {
                val mipmapDir = File(resDir, "mipmap").apply { mkdirs() }
                iconFile.copyTo(File(mipmapDir, iconFile.name), overwrite = true)
            }

            val encoder = ApkModuleXmlEncoder()
            val apkModule = encoder.getApkModule()
            apkModule.addExternalFramework(androidJar)
            encoder.buildResources(stagingDir)

            ResourceCompileResult(success = true, log = "Resources compiled successfully.", apkModule = apkModule)
        } catch (e: Exception) {
            ResourceCompileResult(success = false, log = "Resource compilation failed: ${e.message}")
        }
    }

    private fun readStringNames(stringsFile: File): List<String> {
        if (!stringsFile.isFile) return emptyList()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("name") }
    }

    private fun writePublicXml(file: File, ids: ResourceIds) {
        val text = buildString {
            appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
            appendLine("<resources>")
            ids.strings.forEach { (name, id) ->
                appendLine("""    <public type="string" name="$name" id="0x${id.toString(16)}" />""")
            }
            ids.layouts.forEach { (name, id) ->
                appendLine("""    <public type="layout" name="$name" id="0x${id.toString(16)}" />""")
            }
            ids.mipmaps.forEach { (name, id) ->
                appendLine("""    <public type="mipmap" name="$name" id="0x${id.toString(16)}" />""")
            }
            appendLine("</resources>")
        }
        file.writeText(text)
    }

    private fun writeRJava(generatedSourcesDir: File, packageName: String, ids: ResourceIds) {
        val packageDir = File(generatedSourcesDir, packageName.replace('.', '/')).apply { mkdirs() }
        val text = buildString {
            appendLine("package $packageName;")
            appendLine()
            appendLine("public final class R {")
            appendLine("    public static final class string {")
            ids.strings.forEach { (name, id) -> appendLine("        public static final int $name = 0x${id.toString(16)};") }
            appendLine("    }")
            appendLine("    public static final class layout {")
            ids.layouts.forEach { (name, id) -> appendLine("        public static final int $name = 0x${id.toString(16)};") }
            appendLine("    }")
            appendLine("    public static final class mipmap {")
            ids.mipmaps.forEach { (name, id) -> appendLine("        public static final int $name = 0x${id.toString(16)};") }
            appendLine("    }")
            appendLine("}")
        }
        File(packageDir, "R.java").writeText(text)
    }
}
