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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBuildOutputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val success = intent.getBooleanExtra(EXTRA_SUCCESS, false)
        val log = intent.getStringExtra(EXTRA_LOG).orEmpty()

        binding.textBuildStatus.text = getString(
            if (success) R.string.build_succeeded else R.string.build_failed
        )
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
