package com.javaide.mobile.util

import java.io.File

/**
 * Renames a project's Java package: moves its source directory tree under src/main/java to match
 * the new package, rewrites every moved file's package declaration to match its (possibly nested,
 * for sub-packages) new position, and updates AndroidManifest.xml's package attribute if the
 * project has one (Java-console projects don't).
 */
object PackageRenamer {

    private val PACKAGE_NAME_PATTERN = Regex("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$")
    private val PACKAGE_DECLARATION = Regex("""^package\s+[\w.]+\s*;""", RegexOption.MULTILINE)

    fun isValidPackageName(name: String): Boolean = PACKAGE_NAME_PATTERN.matches(name)

    /** The project's current package, inferred from where its first .java file lives, if any. */
    fun currentPackageName(projectDir: File): String? {
        val javaRoot = File(projectDir, "src/main/java")
        val firstSourceFile = javaRoot.walkTopDown().firstOrNull { it.isFile && it.extension == "java" }
            ?: return null
        return firstSourceFile.parentFile
            ?.relativeTo(javaRoot)
            ?.path
            ?.replace(File.separatorChar, '.')
            ?.takeIf { it.isNotEmpty() }
    }

    fun rename(projectDir: File, oldPackageName: String, newPackageName: String) {
        check(isValidPackageName(newPackageName)) { "Invalid package name: $newPackageName" }

        val javaRoot = File(projectDir, "src/main/java")
        val oldDir = File(javaRoot, oldPackageName.replace('.', '/'))
        check(oldDir.isDirectory) { "Package directory not found: ${oldDir.absolutePath}" }
        val newDir = File(javaRoot, newPackageName.replace('.', '/'))
        check(!newDir.exists()) { "Target package directory already exists: ${newDir.absolutePath}" }

        newDir.parentFile?.mkdirs()
        check(oldDir.renameTo(newDir)) { "Failed to move ${oldDir.absolutePath} to ${newDir.absolutePath}" }
        removeEmptyAncestors(oldDir.parentFile, javaRoot)

        newDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .forEach { file ->
                val correctPackage = file.parentFile!!.relativeTo(javaRoot).path.replace(File.separatorChar, '.')
                val text = file.readText()
                if (PACKAGE_DECLARATION.containsMatchIn(text)) {
                    file.writeText(PACKAGE_DECLARATION.replaceFirst(text, "package $correctPackage;"))
                }
            }

        val manifestFile = File(projectDir, "src/main/AndroidManifest.xml")
        if (manifestFile.isFile) {
            val text = manifestFile.readText()
            manifestFile.writeText(text.replaceFirst("package=\"$oldPackageName\"", "package=\"$newPackageName\""))
        }
    }

    /** Deletes now-empty directories left behind by the move, stopping at (not including) [stopAt]. */
    private fun removeEmptyAncestors(start: File?, stopAt: File) {
        var dir = start
        while (dir != null && dir != stopAt && dir.listFiles()?.isEmpty() == true) {
            val toDelete = dir
            dir = dir.parentFile
            toDelete.delete()
        }
    }
}
