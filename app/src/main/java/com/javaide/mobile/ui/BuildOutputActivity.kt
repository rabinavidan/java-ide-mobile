package com.javaide.mobile.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.javaide.mobile.R
import com.javaide.mobile.databinding.ActivityBuildOutputBinding
import com.javaide.mobile.util.ApkInstaller
import java.io.File

class BuildOutputActivity : BaseActivity() {

    companion object {
        const val EXTRA_SUCCESS = "extra_success"
        const val EXTRA_LOG = "extra_log"
        const val EXTRA_TITLE_RES = "extra_title_res"
        const val EXTRA_SUCCESS_TEXT_RES = "extra_success_text_res"
        const val EXTRA_FAILURE_TEXT_RES = "extra_failure_text_res"
        const val EXTRA_APK_PATH = "extra_apk_path"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBuildOutputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(intent.getIntExtra(EXTRA_TITLE_RES, R.string.title_build_output))

        val success = intent.getBooleanExtra(EXTRA_SUCCESS, false)
        val log = intent.getStringExtra(EXTRA_LOG).orEmpty()
        val apkPath = intent.getStringExtra(EXTRA_APK_PATH)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)

        val successTextRes = intent.getIntExtra(EXTRA_SUCCESS_TEXT_RES, R.string.build_succeeded)
        val failureTextRes = intent.getIntExtra(EXTRA_FAILURE_TEXT_RES, R.string.build_failed)
        binding.textBuildStatus.text = getString(if (success) successTextRes else failureTextRes)
        binding.textBuildStatus.setBackgroundColor(
            if (success) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        )
        binding.textBuildLog.text = log

        if (success && apkPath != null && packageName != null) {
            binding.layoutActions.visibility = View.VISIBLE
            binding.buttonInstall.setOnClickListener {
                ApkInstaller.install(this, File(apkPath))
            }
            binding.buttonLaunch.setOnClickListener {
                ApkInstaller.launch(this, packageName)
            }
            binding.buttonViewLogs.setOnClickListener {
                startActivity(
                    Intent(this, LogcatActivity::class.java)
                        .putExtra(LogcatActivity.EXTRA_PACKAGE_NAME, packageName)
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
