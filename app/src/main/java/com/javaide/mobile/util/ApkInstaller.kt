package com.javaide.mobile.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.javaide.mobile.R
import java.io.File

/** Hands a built APK to the system's package installer, and launches an installed app by package name. */
object ApkInstaller {

    /**
     * Starts the system install flow for [apkFile]. On API 26+, an app must have been granted
     * permission to install unknown apps before this works; if it hasn't, this sends the user
     * to the settings screen to grant it instead (they need to tap Install again afterwards).
     */
    fun install(activity: Activity, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            Toast.makeText(activity, R.string.msg_allow_unknown_sources, Toast.LENGTH_LONG).show()
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${activity.packageName}"))
            activity.startActivity(settingsIntent)
            return
        }

        val uri: Uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.startActivity(installIntent)
    }

    /** Launches [packageName]'s launcher activity, if it's installed. */
    fun launch(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Toast.makeText(context, R.string.error_app_not_installed, Toast.LENGTH_LONG).show()
            return
        }
        context.startActivity(intent)
    }
}
