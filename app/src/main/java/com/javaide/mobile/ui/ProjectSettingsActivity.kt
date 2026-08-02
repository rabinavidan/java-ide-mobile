package com.javaide.mobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.javaide.mobile.R
import com.javaide.mobile.compiler.ManifestUtils
import com.javaide.mobile.data.Logger
import com.javaide.mobile.databinding.ActivityProjectSettingsBinding
import com.javaide.mobile.util.AppNameUtils
import com.javaide.mobile.util.PackageRenamer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lets a project's app name (strings.xml) and Java package be changed after creation -- both are
 * otherwise fixed forever once ProjectTemplate scaffolds them. Package rename moves the source
 * directory tree and rewrites every affected file's package declaration (see PackageRenamer);
 * min/target SDK and app icon are deliberately out of scope for now (see PR description).
 */
class ProjectSettingsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROJECT_PATH = "extra_project_path"
    }

    private lateinit var binding: ActivityProjectSettingsBinding
    private lateinit var projectDir: File
    private lateinit var stringsFile: File
    private var currentPackageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        projectDir = File(intent.getStringExtra(EXTRA_PROJECT_PATH) ?: error("Missing $EXTRA_PROJECT_PATH"))
        stringsFile = File(projectDir, "src/main/res/values/strings.xml")

        val manifestFile = File(projectDir, "src/main/AndroidManifest.xml")
        currentPackageName = if (manifestFile.isFile) {
            ManifestUtils.readPackageName(manifestFile)
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

        binding.buttonSave.setOnClickListener { save() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
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
