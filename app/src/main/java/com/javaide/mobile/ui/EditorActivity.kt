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
import com.javaide.mobile.data.AppDatabase
import com.javaide.mobile.data.EditorState
import com.javaide.mobile.databinding.ActivityEditorBinding
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.java.JavaLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        private const val AUTO_SAVE_DEBOUNCE_MS = 1500L
    }

    private lateinit var binding: ActivityEditorBinding
    private lateinit var file: File
    private var autoSaveJob: Job? = null

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
        restoreCursorPosition()

        binding.codeEditor.subscribeEvent(ContentChangeEvent::class.java) { _, _ -> scheduleAutoSave() }
    }

    override fun onPause() {
        super.onPause()
        autoSaveJob?.cancel()
        lifecycleScope.launch { persistState() }
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

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = lifecycleScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            persistState()
        }
    }

    /** Snapshots the buffer + cursor on the main thread, then writes them off it. */
    private suspend fun persistState() {
        val text = binding.codeEditor.text.toString()
        val line = binding.codeEditor.cursor.leftLine
        val column = binding.codeEditor.cursor.leftColumn
        withContext(Dispatchers.IO) {
            runCatching { file.writeText(text) }
            runCatching {
                AppDatabase.get(this@EditorActivity).dao().upsertEditorState(
                    EditorState(
                        filePath = file.absolutePath,
                        cursorLine = line,
                        cursorColumn = column,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun restoreCursorPosition() {
        lifecycleScope.launch {
            val state = withContext(Dispatchers.IO) {
                AppDatabase.get(this@EditorActivity).dao().getEditorState(file.absolutePath)
            } ?: return@launch
            runCatching { binding.codeEditor.setSelection(state.cursorLine, state.cursorColumn) }
        }
    }
}
