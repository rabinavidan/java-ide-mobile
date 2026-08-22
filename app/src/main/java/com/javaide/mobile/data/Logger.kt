package com.javaide.mobile.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fire-and-forget event logging used from pipeline/UI code (project created, build/run/install
 * outcomes, crashes). Backs the History screen and writes a rotating on-device activity log file.
 */
object Logger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timestampFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private const val MAX_LOG_BYTES = 512 * 1024L // rotate at 512 KB, keep one backup

    fun log(context: Context, level: String, category: String, message: String) {
        val timestamp = System.currentTimeMillis()
        val dao = AppDatabase.get(context).dao()
        scope.launch {
            dao.insertEvent(
                EventEntry(
                    timestamp = timestamp,
                    level = level,
                    category = category,
                    message = message
                )
            )
            appendToFile(context, timestamp, level, category, message)
        }
    }

    fun info(context: Context, category: String, message: String) = log(context, "INFO", category, message)
    fun warn(context: Context, category: String, message: String) = log(context, "WARN", category, message)
    fun error(context: Context, category: String, message: String) = log(context, "ERROR", category, message)

    /** Returns the current activity log file (filesDir/logs/activity.log). */
    fun logFile(context: Context): File = File(context.filesDir, "logs/activity.log")

    private fun appendToFile(context: Context, timestamp: Long, level: String, category: String, message: String) {
        val logsDir = File(context.filesDir, "logs").apply { mkdirs() }
        val logFile = File(logsDir, "activity.log")
        if (logFile.length() > MAX_LOG_BYTES) {
            File(logsDir, "activity.log.1").delete()
            logFile.renameTo(File(logsDir, "activity.log.1"))
        }
        logFile.appendText("[${timestampFmt.format(Date(timestamp))}] [$level] [$category] $message\n")
    }
}
