package com.javaide.mobile.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.javaide.mobile.R
import com.javaide.mobile.databinding.ActivityLogcatBinding
import com.javaide.mobile.util.LogcatReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shows an installed app's recent logcat output (see LogcatReader for why this only works on
 * rooted devices, emulators, and other dev-friendly builds -- Android restricts logcat to an
 * app's own UID since API 16, so a locked-down device will show [msg_logcat_no_process] instead).
 */
class LogcatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    private lateinit var binding: ActivityLogcatBinding
    private lateinit var targetPackageName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogcatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.title_logcat)

        targetPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: error("Missing $EXTRA_PACKAGE_NAME")
        binding.buttonRefresh.setOnClickListener { loadLogs() }
        loadLogs()
    }

    private fun loadLogs() {
        binding.textLogcatMessage.visibility = View.GONE
        binding.textLogcatOutput.text = ""
        lifecycleScope.launch {
            when (val result = withContext(Dispatchers.IO) { LogcatReader.read(targetPackageName) }) {
                is LogcatReader.Result.NoProcessFound -> {
                    binding.textLogcatMessage.visibility = View.VISIBLE
                    binding.textLogcatMessage.text = getString(R.string.msg_logcat_no_process)
                }
                is LogcatReader.Result.Entries -> {
                    if (result.entries.isEmpty()) {
                        binding.textLogcatMessage.visibility = View.VISIBLE
                        binding.textLogcatMessage.text = getString(R.string.msg_logcat_empty)
                    } else {
                        binding.textLogcatOutput.text = result.entries.joinToString("\n") {
                            "${it.level}/${it.tag}: ${it.message}"
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
