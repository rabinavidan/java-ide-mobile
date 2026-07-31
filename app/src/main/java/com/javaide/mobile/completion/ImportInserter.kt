package com.javaide.mobile.completion

/**
 * Works out which imports still need adding and where to add them for a piece of Java source.
 * Deliberately returns a line position rather than doing a full-text replace, so the caller can
 * use CodeEditor's own `Content.insert(line, column, text)` and keep cursor position/undo history
 * intact instead of resetting the whole buffer.
 */
object ImportInserter {

    private val PACKAGE_LINE = Regex("""^\s*package\s+[\w.]+\s*;\s*$""")
    private val IMPORT_LINE = Regex("""^\s*import\s+(static\s+)?([\w.]+)\s*;\s*$""")

    /** Names from [fullyQualifiedNames] not already imported in [source], deduped and sorted. */
    fun pendingImports(source: String, fullyQualifiedNames: List<String>): List<String> {
        val alreadyImported = source.lineSequence()
            .mapNotNull { IMPORT_LINE.matchEntire(it)?.groupValues?.get(2) }
            .toSet()
        return fullyQualifiedNames.distinct().filterNot { it in alreadyImported }.sorted()
    }

    /** 0-based line to insert new import lines at: right after the last package/import line. */
    fun insertionLine(source: String): Int {
        var insertAt = 0
        source.lines().forEachIndexed { index, line ->
            if (PACKAGE_LINE.matches(line) || IMPORT_LINE.matches(line)) {
                insertAt = index + 1
            }
        }
        return insertAt
    }
}
