package com.javaide.mobile.util

import java.io.File

/** File/folder create, rename, and delete operations for the file explorer. */
object FileOps {

    fun isValidFileName(name: String): Boolean =
        name.isNotBlank() && !name.contains('/') && !name.contains('\\') && name != "." && name != ".."

    fun createFolder(parent: File, name: String): File {
        val dir = File(parent, name)
        check(!dir.exists()) { "Already exists: $name" }
        check(dir.mkdirs()) { "Failed to create folder: $name" }
        return dir
    }

    /** Creates a new file in [parent]; a ".java" name is seeded with a package-aware class skeleton. */
    fun createFile(projectDir: File, parent: File, name: String): File {
        val target = File(parent, name)
        check(!target.exists()) { "Already exists: $name" }
        parent.mkdirs()
        if (name.endsWith(".java")) {
            val className = name.removeSuffix(".java")
            target.writeText(javaClassSkeleton(inferPackage(projectDir, parent), className))
        } else {
            check(target.createNewFile()) { "Failed to create file: $name" }
        }
        return target
    }

    /** Renames a file or folder in place, keeping it in the same parent directory. */
    fun renameEntry(entry: File, newName: String): File {
        val target = File(entry.parentFile, newName)
        check(!target.exists()) { "Already exists: $newName" }
        check(entry.renameTo(target)) { "Failed to rename ${entry.name}" }
        return target
    }

    /** Deletes a file, or a folder and everything under it. */
    fun deleteEntry(entry: File) {
        check(entry.deleteRecursively()) { "Failed to delete ${entry.name}" }
    }

    /** The Java package implied by [dir]'s position under [projectDir]'s "src/main/java", if any. */
    private fun inferPackage(projectDir: File, dir: File): String? {
        val javaRootPath = File(projectDir, "src/main/java").absolutePath
        val dirPath = dir.absolutePath
        // Boundary-checked, not a bare startsWith: ".../src/main/javaExtra" must not match.
        if (dirPath != javaRootPath && !dirPath.startsWith(javaRootPath + File.separator)) return null
        return dirPath.removePrefix(javaRootPath)
            .trim(File.separatorChar)
            .replace(File.separatorChar, '.')
            .takeIf { it.isNotEmpty() }
    }

    private fun javaClassSkeleton(packageName: String?, className: String): String {
        val packageLine = packageName?.let { "package $it;\n\n" }.orEmpty()
        return "${packageLine}public class $className {\n}\n"
    }
}
