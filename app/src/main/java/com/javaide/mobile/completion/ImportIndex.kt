package com.javaide.mobile.completion

import java.io.File
import java.util.zip.ZipFile

/**
 * Maps a simple class name (e.g. "ArrayList") to its candidate fully-qualified names, indexed
 * from android.jar's own class entries -- since android.jar already bundles the full compile-time
 * surface (android.*, java.*, javax.*) this app compiles against, no separate JDK index is needed.
 * Nested/inner classes (entries containing '$') are skipped for now: importing them needs
 * "import a.b.Outer.Inner;" syntax, a case deliberately left out of this first version.
 */
object ImportIndex {

    @Volatile
    private var cache: Map<String, List<String>>? = null

    fun candidatesFor(androidJar: File, simpleName: String): List<String> =
        indexFor(androidJar)[simpleName].orEmpty()

    private fun indexFor(androidJar: File): Map<String, List<String>> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            return build(androidJar).also { cache = it }
        }
    }

    private fun build(androidJar: File): Map<String, List<String>> {
        val index = LinkedHashMap<String, MutableList<String>>()
        ZipFile(androidJar).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
                if (!name.endsWith(".class") || name.contains('$')) continue
                val qualifiedName = name.removeSuffix(".class").replace('/', '.')
                val simpleName = qualifiedName.substringAfterLast('.')
                index.getOrPut(simpleName) { mutableListOf() }.add(qualifiedName)
            }
        }
        return index
    }
}
