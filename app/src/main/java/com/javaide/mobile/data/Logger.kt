package com.javaide.mobile.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fire-and-forget event logging used from pipeline/UI code (project created, build/run/install
 * outcomes, crashes). Backs the History screen and doesn't block the caller.
 */
object Logger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun log(context: Context, level: String, category: String, message: String) {
        val dao = AppDatabase.get(context).dao()
        scope.launch {
            dao.insertEvent(
                EventEntry(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    category = category,
                    message = message
                )
            )
        }
    }

    fun info(context: Context, category: String, message: String) = log(context, "INFO", category, message)
    fun warn(context: Context, category: String, message: String) = log(context, "WARN", category, message)
    fun error(context: Context, category: String, message: String) = log(context, "ERROR", category, message)
}
