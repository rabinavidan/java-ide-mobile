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
}
