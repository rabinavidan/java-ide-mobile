package com.javaide.mobile.compiler

import android.content.Context
import java.io.File

/** Extracts the bundled android.jar (see app/build.gradle.kts) to app-private storage. */
object AndroidJarProvider {

    private const val ASSET_NAME = "android.jar"

    fun get(context: Context): File {
        val target = File(File(context.filesDir, "sdk").apply { mkdirs() }, ASSET_NAME)
        if (!target.exists()) {
            context.assets.open(ASSET_NAME).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return target
    }
}
