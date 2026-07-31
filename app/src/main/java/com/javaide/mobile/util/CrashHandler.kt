package com.javaide.mobile.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures uncaught exceptions to a plain file (no Room, no coroutines) since the process may be
 * in an unstable state right before it dies -- a database write could itself fail or block.
 * The file is reconciled into the events table on the next app launch, see IdeApplication.
 */
object CrashHandler {

    private const val PENDING_CRASH_FILE = "pending_crash.log"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrash(appContext, thread, throwable)
            } catch (_: Throwable) {
                // Never let crash logging itself break the crash handler.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        pendingCrashFile(context).appendText("[$timestamp] Thread: ${thread.name}\n$stackTrace\n")
    }

    private fun pendingCrashFile(context: Context): File = File(context.filesDir, PENDING_CRASH_FILE)

    /** Returns and clears any crash recorded before the last unexpected process death, if present. */
    fun consumePendingCrash(context: Context): String? {
        val file = pendingCrashFile(context)
        if (!file.exists() || file.length() == 0L) return null
        val text = file.readText()
        file.delete()
        return text
    }
}
