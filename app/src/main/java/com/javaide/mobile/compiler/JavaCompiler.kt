package com.javaide.mobile.compiler

import org.eclipse.jdt.internal.compiler.batch.Main
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

data class CompileResult(val success: Boolean, val log: String)

/** Compiles a project's Java sources to .class files on-device using ECJ. */
object JavaCompiler {

    fun compile(
        projectDir: File,
        outputDir: File,
        androidJar: File,
        extraSourceDirs: List<File> = emptyList()
    ): CompileResult {
        val sourceFiles = (listOf(File(projectDir, "src/main/java")) + extraSourceDirs)
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "java" } }
            .map { it.absolutePath }
            .toList()

        if (sourceFiles.isEmpty()) {
            return CompileResult(success = false, log = "No .java files found under src/main/java")
        }

        outputDir.mkdirs()

        val outWriter = StringWriter()
        val errWriter = StringWriter()

        val args = arrayOf(
            "-source", "1.8",
            "-target", "1.8",
            "-encoding", "UTF-8",
            "-proc:none",
            "-nowarn",
            "-d", outputDir.absolutePath,
            "-cp", androidJar.absolutePath
        ) + sourceFiles

        // systemExitWhenFinished must be false: the default calls System.exit() on
        // failure, which would kill the host app instead of just failing the build.
        val compiler = Main(PrintWriter(outWriter), PrintWriter(errWriter), false, null, null)
        val success = compiler.compile(args)

        val log = (outWriter.toString() + errWriter.toString())
            .ifBlank { if (success) "Compilation succeeded." else "Compilation failed." }

        return CompileResult(success = success, log = log)
    }
}
