package com.javaide.mobile.compiler

import java.io.File

/**
 * Third-party .jar dependencies for a project: any .jar dropped into a "libs" folder at the
 * project root (the same convention pre-Gradle Android tooling used) is picked up automatically
 * by the compile/dex/diagnostics/completion pipeline -- no project-template change needed.
 */
object LibJars {

    fun jarsIn(projectDir: File): List<File> =
        File(projectDir, "libs")
            .listFiles { file -> file.isFile && file.extension == "jar" }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
}
