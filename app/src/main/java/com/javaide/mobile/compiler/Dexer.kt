package com.javaide.mobile.compiler

import com.android.tools.r8.CompilationFailedException
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.Diagnostic
import com.android.tools.r8.DiagnosticsHandler
import com.android.tools.r8.OutputMode
import java.io.File

data class DexResult(val success: Boolean, val log: String)

/** Converts .class files produced by [JavaCompiler] into classes.dex using D8. */
object Dexer {

    private const val MIN_API_LEVEL = 26

    fun dex(classesDir: File, outputDir: File, androidJar: File): DexResult {
        val classFiles = classesDir.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .map { it.toPath() }
            .toList()

        if (classFiles.isEmpty()) {
            return DexResult(success = false, log = "No .class files found to dex.")
        }

        outputDir.mkdirs()

        val messages = StringBuilder()
        val diagnosticsHandler = object : DiagnosticsHandler {
            override fun error(diagnostic: Diagnostic) {
                messages.appendLine("error: ${diagnostic.diagnosticMessage}")
            }

            override fun warning(diagnostic: Diagnostic) {
                messages.appendLine("warning: ${diagnostic.diagnosticMessage}")
            }
        }

        return try {
            val command = D8Command.builder(diagnosticsHandler)
                .addProgramFiles(classFiles)
                .addLibraryFiles(androidJar.toPath())
                .setOutput(outputDir.toPath(), OutputMode.DexIndexed)
                .setMinApiLevel(MIN_API_LEVEL)
                .build()
            D8.run(command)
            DexResult(success = true, log = messages.toString().ifBlank { "Dexing succeeded." })
        } catch (e: CompilationFailedException) {
            DexResult(success = false, log = messages.toString().ifBlank { e.message ?: "Dexing failed." })
        }
    }
}
