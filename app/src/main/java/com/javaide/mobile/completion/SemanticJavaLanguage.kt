package com.javaide.mobile.completion

import android.os.Bundle
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import java.io.File

/**
 * Extends sora-editor's stock JavaLanguage (keyword + in-file-identifier completion) with
 * ECJ-backed semantic completion: dot-member lookups and in-scope locals/fields, resolved
 * against android.jar and the file's own declarations. See SemanticCompletionEngine for how.
 */
class SemanticJavaLanguage(private val androidJar: File) : JavaLanguage() {

    var fileName: String = "Source.java"

    /** The project's compiled-classes output dir, so cross-file symbols can resolve. Set once known. */
    var projectClassesDir: File? = null

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        super.requireAutoComplete(content, position, publisher, extraArguments)

        val text = content.toString()
        val cursor = content.getCharIndex(position.line, position.column)

        val result = try {
            SemanticCompletionEngine.complete(androidJar, text, cursor, fileName, projectClassesDir)
        } catch (e: Exception) {
            null
        } ?: return

        for (suggestion in result.suggestions) {
            val kind = when (suggestion.kind) {
                SemanticKind.FIELD -> CompletionItemKind.Field
                SemanticKind.METHOD -> CompletionItemKind.Method
                SemanticKind.LOCAL -> CompletionItemKind.Variable
            }
            val label = if (suggestion.kind == SemanticKind.METHOD) "${suggestion.name}()" else suggestion.name
            publisher.addItem(
                SimpleCompletionItem(label, suggestion.detail, result.prefixLength, suggestion.name)
                    .kind(kind)
            )
        }
        publisher.updateList()
    }
}
