package com.javaide.mobile.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/** Saves a user-picked image as a project's app icon, always at a fixed, uniform size. */
object IconUtils {

    private const val ICON_SIZE_PX = 192

    /**
     * Decodes [uri]'s image content and scales it to a fixed [ICON_SIZE_PX] square before saving
     * it as a PNG at [target] -- regardless of what was picked (a full-resolution photo, a tiny
     * image, a non-square crop), so the built app always gets a reasonably-sized, uniform launcher
     * icon instead of bloating the APK with whatever the user happened to pick. A non-square
     * source image is stretched to fill the square, not center-cropped.
     */
    fun saveResizedIcon(context: Context, uri: Uri, target: File) {
        val original = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: error("Could not decode the selected image")
        val scaled = Bitmap.createScaledBitmap(original, ICON_SIZE_PX, ICON_SIZE_PX, true)
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { out -> scaled.compress(Bitmap.CompressFormat.PNG, 100, out) }
    }
}
