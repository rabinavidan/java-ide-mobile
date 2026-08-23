package com.javaide.mobile.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.javaide.mobile.R
import com.javaide.mobile.compiler.ManifestUtils
import com.javaide.mobile.compiler.ResourceCompiler
import com.javaide.mobile.data.Logger
import com.javaide.mobile.databinding.ActivityProjectSettingsBinding
import com.javaide.mobile.util.AppNameUtils
import com.javaide.mobile.util.IconUtils
import com.javaide.mobile.util.PackageRenamer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lets a project's app name (strings.xml), Java package, min/target SDK, and app icon be changed
 * after creation -- all otherwise fixed forever once ProjectTemplate scaffolds them. Package
 * rename moves the source directory tree and rewrites every affected file's package declaration
 * (see PackageRenamer). Min/target SDK and the icon only apply to Android-app projects (Java-
 * console projects have no AndroidManifest.xml at all), so that whole section is hidden for those.
 */
class ProjectSettingsActivity : BaseActivity() {

    companion object {
        const val EXTRA_PROJECT_PATH = "extra_project_path"
    }

    private lateinit var binding: ActivityProjectSettingsBinding
    private lateinit var projectDir: File
    private lateinit var stringsFile: File
    private var manifestFile: File? = null
    private var currentPackageName: String? = null

    private val chooseIconLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importIcon(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        projectDir = File(intent.getStringExtra(EXTRA_PROJECT_PATH) ?: error("Missing $EXTRA_PROJECT_PATH"))
        stringsFile = File(projectDir, "src/main/res/values/strings.xml")

        val manifest = File(projectDir, "src/main/AndroidManifest.xml").takeIf { it.isFile }
        manifestFile = manifest
        currentPackageName = if (manifest != null) {
            ManifestUtils.readPackageName(manifest)
        } else {
            PackageRenamer.currentPackageName(projectDir)
        }
        binding.editPackageName.setText(currentPackageName.orEmpty())

        val appName = AppNameUtils.read(stringsFile)
        if (appName != null) {
            binding.editAppName.setText(appName)
        } else {
            binding.labelAppName.visibility = View.GONE
            binding.editAppName.visibility = View.GONE
        }

        if (manifest != null) {
            binding.editMinSdk.setText((ManifestUtils.readMinSdkVersion(manifest) ?: 26).toString())
            binding.editTargetSdk.setText((ManifestUtils.readTargetSdkVersion(manifest) ?: 34).toString())
            binding.buttonChooseIcon.setOnClickListener { chooseIconLauncher.launch(arrayOf("image/*")) }
            iconFile().takeIf { it.isFile }?.let { showIconPreview(it) }
        } else {
            binding.sectionSdkAndIcon.visibility = View.GONE
        }

        binding.buttonSave.setOnClickListener { save() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun iconFile() = File(projectDir, "src/main/res/mipmap/${ResourceCompiler.ICON_RESOURCE_NAME}.png")

    private fun showIconPreview(file: File) {
        binding.imageIconPreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        binding.imageIconPreview.visibility = View.VISIBLE
    }

    private fun importIcon(uri: Uri) {
        val manifest = manifestFile ?: return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val target = iconFile()
                    IconUtils.saveResizedIcon(this@ProjectSettingsActivity, uri, target)
                    ManifestUtils.writeIcon(manifest, ResourceCompiler.ICON_RESOURCE_NAME)
                    target
                }
            }
            result
                .onSuccess { target ->
                    showIconPreview(target)
                    Logger.info(this@ProjectSettingsActivity, "settings", "Updated app icon for '${projectDir.name}'")
                    Toast.makeText(this@ProjectSettingsActivity, R.string.msg_icon_updated, Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Logger.error(this@ProjectSettingsActivity, "settings", "Failed to update app icon: ${it.message}")
                    Toast.makeText(this@ProjectSettingsActivity, R.string.error_icon_update_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun save() {
        val newPackageName = binding.editPackageName.text?.toString()?.trim().orEmpty()
        val oldPackageName = currentPackageName

        if (oldPackageName != null && newPackageName != oldPackageName) {
            if (!PackageRenamer.isValidPackageName(newPackageName)) {
                Toast.makeText(this, R.string.error_invalid_package_name, Toast.LENGTH_SHORT).show()
                return
            }
        }

        val manifest = manifestFile
        // Pair, not two nullable vars: a var captured across the coroutine below can't be
        // smart-cast back to non-null inside it, since Kotlin can't rule out reassignment from
        // another thread in the meantime -- a single val sidesteps that entirely.
        val sdkVersions: Pair<Int, Int>? = if (manifest != null) {
            val minSdk = binding.editMinSdk.text?.toString()?.toIntOrNull()
            val targetSdk = binding.editTargetSdk.text?.toString()?.toIntOrNull()
            if (minSdk == null || targetSdk == null || minSdk <= 0 || targetSdk < minSdk) {
                Toast.makeText(this, R.string.error_invalid_sdk_versions, Toast.LENGTH_SHORT).show()
                return
            }
            minSdk to targetSdk
        } else {
            null
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (oldPackageName != null && newPackageName != oldPackageName) {
                        PackageRenamer.rename(projectDir, oldPackageName, newPackageName)
                    }
                    if (binding.editAppName.visibility == View.VISIBLE) {
                        val newAppName = binding.editAppName.text?.toString().orEmpty()
                        if (newAppName.isNotEmpty()) {
                            AppNameUtils.write(stringsFile, newAppName)
                        }
                    }
                    if (manifest != null && sdkVersions != null) {
                        ManifestUtils.writeSdkVersions(manifest, sdkVersions.first, sdkVersions.second)
                    }
                }
            }
            result
                .onSuccess {
                    Logger.info(this@ProjectSettingsActivity, "settings", "Updated project settings for '${projectDir.name}'")
                    Toast.makeText(this@ProjectSettingsActivity, R.string.msg_project_settings_saved, Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure {
                    Logger.error(this@ProjectSettingsActivity, "settings", "Failed to save project settings: ${it.message}")
                    Toast.makeText(this@ProjectSettingsActivity, R.string.error_project_settings_save_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }
}
