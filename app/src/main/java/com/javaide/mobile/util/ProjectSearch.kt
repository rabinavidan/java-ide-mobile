package com.javaide.mobile.util

import java.io.File

/** One matching line: [lineNumber] is 1-based (for display), matching typical editor conventions. */
data class SearchMatch(val file: File, val lineNumber: Int, val lineText: String)

/** Groups matches under the same file, in the order files were found. */
data class SearchFileResult(val file: File, val matches: List<SearchMatch>)

/** Recursive plain-text, line-based search across a project directory. */
object ProjectSearch {

    private val SKIP_DIR_NAMES = setOf("build", ".git")

    fun search(projectDir: File, query: String, caseSensitive: Boolean): List<SearchFileResult> {
        if (query.isEmpty()) return emptyList()

        val matchesByFile = LinkedHashMap<File, MutableList<SearchMatch>>()
        projectDir.walkTopDown()
            .onEnter { it.name !in SKIP_DIR_NAMES }
            .filter { it.isFile }
            .forEach { file ->
                runCatching {
                    file.readLines().forEachIndexed { index, line ->
                        val matched = if (caseSensitive) line.contains(query) else line.contains(query, ignoreCase = true)
                        if (matched) {
                            matchesByFile.getOrPut(file) { mutableListOf() }
                                .add(SearchMatch(file, index + 1, line.trim()))
                        }
                    }
                }
            }
        return matchesByFile.map { (file, matches) -> SearchFileResult(file, matches) }
    }

    /**
     * Replaces every occurrence of [query] with [replacement] in every file under [projectDir]
     * that contains it (skipping build/.git), the same substring matching [search] itself uses --
     * not whole-word, so a query that's part of a larger identifier replaces that part too, same
     * as [search] would highlight it as a match. Returns how many files were changed.
     */
    fun replaceAll(projectDir: File, query: String, replacement: String, caseSensitive: Boolean): Int {
        if (query.isEmpty()) return 0

        var changedFiles = 0
        projectDir.walkTopDown()
            .onEnter { it.name !in SKIP_DIR_NAMES }
            .filter { it.isFile }
            .forEach { file ->
                val text = runCatching { file.readText() }.getOrNull() ?: return@forEach
                val matched = if (caseSensitive) text.contains(query) else text.contains(query, ignoreCase = true)
                if (!matched) return@forEach
                file.writeText(text.replace(query, replacement, ignoreCase = !caseSensitive))
                changedFiles++
            }
        return changedFiles
    }
}
