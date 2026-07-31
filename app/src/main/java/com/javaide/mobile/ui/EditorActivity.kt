package com.javaide.mobile.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.javaide.mobile.R
import com.javaide.mobile.compiler.AndroidJarProvider
import com.javaide.mobile.completion.SemanticJavaLanguage
import com.javaide.mobile.databinding.ActivityEditorBinding
import io.github.rosemoe.sora.langs.java.JavaLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    private lateinit var binding: ActivityEditorBinding
    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val path = intent.getStringExtra(EXTRA_FILE_PATH) ?: error("Missing $EXTRA_FILE_PATH")
        file = File(path)
        title = file.name

        if (file.extension == "java") {
            binding.codeEditor.setEditorLanguage(JavaLanguage())
            lifecycleScope.launch {
                val androidJar = withContext(Dispatchers.IO) { AndroidJarProvider.get(this@EditorActivity) }
                binding.codeEditor.setEditorLanguage(
                    SemanticJavaLanguage(androidJar).apply { fileName = file.name }
                )
            }
        }
        binding.codeEditor.setText(file.readText())
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_save) {
            saveFile()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveFile() {
        runCatching { file.writeText(binding.codeEditor.text.toString()) }
            .onSuccess { Toast.makeText(this, R.string.file_saved, Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(this, R.string.file_save_failed, Toast.LENGTH_SHORT).show() }
    }
}
