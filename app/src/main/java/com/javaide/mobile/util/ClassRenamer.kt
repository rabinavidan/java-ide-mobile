package com.javaide.mobile.util

import java.io.File

/**
 * Renames a top-level Java type (class/interface/enum) and propagates the rename to whole-word
 * usages across the rest of the project -- constructor calls, type references, import statements.
 * Text-based (whole-word regex substitution), not ECJ binding resolution: a comment/string
 * mentioning the old name, or an unrelated identifier that happens to share it, would also be
 * rewritten. A real, documented limitation, not something a proper "find references" engine would
 * have -- accepted here as a much smaller feature than building that engine from scratch.
 */
object ClassRenamer {

    private val SKIP_DIR_NAMES = setOf("build", ".git")

    /**
     * Whether [file] itself declares a top-level class/interface/enum named [expectedName] --
     * i.e. whether renaming this file is really a class rename, not just some other .java file
     * whose contents don't actually match its old filename.
     */
    fun isClassDeclaration(file: File, expectedName: String): Boolean {
        if (!file.isFile || file.extension != "java") return false
        val text = runCatching { file.readText() }.getOrNull() ?: return false
        val declaration = Regex("""\b(?:class|interface|enum)\s+${Regex.escape(expectedName)}\b""")
        return declaration.containsMatchIn(text)
    }

    /**
     * Rewrites every whole-word occurrence of [oldName] to [newName] in every .java file under
     * [projectDir] (skipping build/.git), including the declaration itself. Returns how many
     * files were changed.
     */
    fun renameAcrossProject(projectDir: File, oldName: String, newName: String): Int {
        val wordBoundary = Regex("""\b${Regex.escape(oldName)}\b""")
        var changedFiles = 0

        projectDir.walkTopDown()
            .onEnter { it.name !in SKIP_DIR_NAMES }
            .filter { it.isFile && it.extension == "java" }
            .forEach { javaFile ->
                val text = runCatching { javaFile.readText() }.getOrNull() ?: return@forEach
                if (!wordBoundary.containsMatchIn(text)) return@forEach
                javaFile.writeText(wordBoundary.replace(text, newName))
                changedFiles++
            }

        return changedFiles
    }
}
