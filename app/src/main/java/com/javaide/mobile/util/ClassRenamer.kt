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

    /** A type's declaring file and 0-based declaration line, for jumping to it in the editor. */
    data class TypeLocation(val file: File, val lineNumber: Int)

    /**
     * Finds which .java file under [projectDir] declares a top-level class/interface/enum named
     * [simpleName], for cross-file "Go to Definition" jumps (see DefinitionFinder). Name-based,
     * like [isClassDeclaration] itself -- not exact reference resolution, so an unrelated type that
     * happens to share the name (in a different project than intended, or after a stale rename)
     * would be found instead; accepted for the same reasons as this class's other limitations.
     */
    fun findDeclaration(projectDir: File, simpleName: String): TypeLocation? {
        val declaration = Regex("""\b(?:class|interface|enum)\s+${Regex.escape(simpleName)}\b""")
        projectDir.walkTopDown()
            .onEnter { it.name !in SKIP_DIR_NAMES }
            .filter { it.isFile && it.extension == "java" }
            .forEach { file ->
                val text = runCatching { file.readText() }.getOrNull() ?: return@forEach
                val match = declaration.find(text) ?: return@forEach
                val lineNumber = text.substring(0, match.range.first).count { it == '\n' }
                return TypeLocation(file, lineNumber)
            }
        return null
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
