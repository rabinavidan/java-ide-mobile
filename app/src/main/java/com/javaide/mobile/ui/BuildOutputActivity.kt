package com.javaide.mobile.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.javaide.mobile.R
import com.javaide.mobile.databinding.ActivityBuildOutputBinding

class BuildOutputActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SUCCESS = "extra_success"
        const val EXTRA_LOG = "extra_log"
        const val EXTRA_TITLE_RES = "extra_title_res"
        const val EXTRA_SUCCESS_TEXT_RES = "extra_success_text_res"
        const val EXTRA_FAILURE_TEXT_RES = "extra_failure_text_res"
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

        val successTextRes = intent.getIntExtra(EXTRA_SUCCESS_TEXT_RES, R.string.build_succeeded)
        val failureTextRes = intent.getIntExtra(EXTRA_FAILURE_TEXT_RES, R.string.build_failed)
        binding.textBuildStatus.text = getString(if (success) successTextRes else failureTextRes)
        binding.textBuildStatus.setBackgroundColor(
            if (success) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        )
        binding.textBuildLog.text = log
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
