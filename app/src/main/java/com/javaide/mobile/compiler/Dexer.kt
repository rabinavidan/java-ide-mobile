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

    /** Fallback when a project has no manifest to read a minSdk from (e.g. a Java-console project). */
    const val DEFAULT_MIN_API_LEVEL = 26

    /**
     * [libJars]: third-party dependency jars (see [LibJars]). Unlike [androidJar] -- which is
     * provided by the OS at runtime and only ever used as a library reference -- these need to be
     * dexed and merged into the app's own output, since nothing else will bundle them. D8's
     * addProgramFiles already handles a .jar path directly, extracting and dexing its classes
     * alongside the project's own, the same way a real Android build merges dependency jars in.
     *
     * [minApiLevel]: the project's manifest minSdkVersion (see ManifestUtils.readMinSdkVersion),
     * or [DEFAULT_MIN_API_LEVEL] if it has none. D8 itself rejects a nonsensical value through the
     * usual [DexResult] failure log, so this doesn't pre-validate it.
     */
    fun dex(
        classesDir: File,
        outputDir: File,
        androidJar: File,
        libJars: List<File> = emptyList(),
        minApiLevel: Int = DEFAULT_MIN_API_LEVEL
    ): DexResult {
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
                .addProgramFiles(libJars.map { it.toPath() })
                .addLibraryFiles(androidJar.toPath())
                .setOutput(outputDir.toPath(), OutputMode.DexIndexed)
                .setMinApiLevel(minApiLevel)
                .build()
            D8.run(command)
            DexResult(success = true, log = messages.toString().ifBlank { "Dexing succeeded." })
        } catch (e: CompilationFailedException) {
            DexResult(success = false, log = messages.toString().ifBlank { e.message ?: "Dexing failed." })
        }
    }
}
