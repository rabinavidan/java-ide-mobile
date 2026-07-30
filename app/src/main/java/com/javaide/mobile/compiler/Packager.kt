package com.javaide.mobile.compiler

import com.android.apksig.ApkSigner
import com.reandroid.apk.ApkModule
import com.reandroid.archive.FileInputSource
import java.io.File

data class PackageResult(val success: Boolean, val log: String, val apkFile: File? = null)

/** Adds the dexed classes to a resource-compiled [ApkModule] and signs the result. */
object Packager {

    private const val MIN_SDK_VERSION = 26

    fun packageApk(apkModule: ApkModule, dexFile: File, unsignedApk: File, signedApk: File): PackageResult {
        return try {
            apkModule.add(FileInputSource(dexFile, "classes.dex"))
            unsignedApk.parentFile?.mkdirs()
            apkModule.writeApk(unsignedApk)

            val (privateKey, certificate) = DebugSigningKey.getOrCreate()
            val signerConfig = ApkSigner.SignerConfig.Builder(
                "javaide-debug", privateKey, listOf(certificate)
            ).build()

            signedApk.parentFile?.mkdirs()
            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(unsignedApk)
                .setOutputApk(signedApk)
                .setMinSdkVersion(MIN_SDK_VERSION)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
                .sign()

            PackageResult(success = true, log = "APK written to ${signedApk.absolutePath}", apkFile = signedApk)
        } catch (e: Exception) {
            PackageResult(success = false, log = "Packaging failed: ${e.message}")
        }
    }
}
