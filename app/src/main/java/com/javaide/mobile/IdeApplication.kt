package com.javaide.mobile

import android.app.Application
import com.javaide.mobile.data.Logger
import com.javaide.mobile.util.CrashHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IdeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)

        CoroutineScope(Dispatchers.IO).launch {
            CrashHandler.consumePendingCrash(this@IdeApplication)?.let { crashText ->
                Logger.error(this@IdeApplication, "crash", crashText)
            }
        }
    }
}
