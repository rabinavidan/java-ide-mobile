package com.javaide.mobile.compiler

import org.eclipse.jdt.internal.compiler.batch.Main
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
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
        extraSourceDirs: List<File> = emptyList(),
        libJars: List<File> = emptyList()
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

        val classpath = (listOf(androidJar) + libJars).joinToString(File.pathSeparator) { it.absolutePath }

        val args = arrayOf(
            "-encoding", "UTF-8",
            "-proc:none",
            "-nowarn",
            "-d", outputDir.absolutePath,
            "-cp", classpath
        ) + sourceFiles

        // systemExitWhenFinished must be false: the default calls System.exit() on
        // failure, which would kill the host app instead of just failing the build.
        // Wrapped in try/catch(Throwable): ECJ may reference missing Android runtime
        // classes — catch Errors too.
        //
        // Root cause of Runtime.Version crash on Android:
        // ECJ's Main.initialize() seeds this.options from new CompilerOptions().getMap(),
        // which always includes OPTION_Source="1.8". CompilerOptions.readFromMap() then
        // sets requestedSourceVersion="1.8", and Parser.parse() calls
        // Runtime.Version.parse(requestedSourceVersion) — a class that doesn't exist on
        // Android. Passing OPTION_Source="" as a custom default overrides this to a blank
        // string; the guard `!requestedSourceVersion.isBlank()` is false, so
        // Version.parse() is never called. versionToJdkLevel("") returns 0 so all
        // downstream source/target/compliance checks are safely skipped.
        val customOptions = hashMapOf(CompilerOptions.OPTION_Source to "")
        return try {
            val compiler = Main(PrintWriter(outWriter), PrintWriter(errWriter), false, customOptions, null)
            val success = compiler.compile(args)
            val log = (outWriter.toString() + errWriter.toString())
                .ifBlank { if (success) "Compilation succeeded." else "Compilation failed." }
            CompileResult(success = success, log = log)
        } catch (e: Throwable) {
            CompileResult(success = false, log = "Compiler error: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
