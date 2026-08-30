package com.javaide.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.javaide.mobile.databinding.ActivityHelpBinding

/** Static, always-available guide to what the app can do -- not a first-run-only tour. */
class HelpActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
