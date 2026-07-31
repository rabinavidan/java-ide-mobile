package com.javaide.mobile.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.javaide.mobile.R
import com.javaide.mobile.compiler.AndroidJarProvider
import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.completion.DiagnosticSeverity
import com.javaide.mobile.completion.DiagnosticsEngine
import com.javaide.mobile.completion.SemanticJavaLanguage
import com.javaide.mobile.data.AppDatabase
import com.javaide.mobile.data.EditorState
import com.javaide.mobile.databinding.ActivityEditorBinding
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.EditorSearcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_PROJECT_PATH = "extra_project_path"
        private const val AUTO_SAVE_DEBOUNCE_MS = 1500L
        private const val DIAGNOSTICS_DEBOUNCE_MS = 1500L
    }

    private lateinit var binding: ActivityEditorBinding
    private lateinit var file: File
    private var autoSaveJob: Job? = null
    private var diagnosticsJob: Job? = null
    private var androidJarFile: File? = null
    private var projectClassesDir: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val path = intent.getStringExtra(EXTRA_FILE_PATH) ?: error("Missing $EXTRA_FILE_PATH")
        file = File(path)
        title = file.name
        val projectPath = intent.getStringExtra(EXTRA_PROJECT_PATH)

        if (file.extension == "java") {
            binding.codeEditor.setEditorLanguage(JavaLanguage())
            lifecycleScope.launch {
                val androidJar = withContext(Dispatchers.IO) { AndroidJarProvider.get(this@EditorActivity) }
                androidJarFile = androidJar
                val language = SemanticJavaLanguage(androidJar).apply { fileName = file.name }
                binding.codeEditor.setEditorLanguage(language)
                runDiagnostics()

                if (projectPath != null) {
                    seedProjectClassesForCompletion(File(projectPath), androidJar, language)
                }
            }
        }
        binding.codeEditor.setText(file.readText())
        restoreCursorPosition()

        binding.codeEditor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            scheduleAutoSave()
            scheduleDiagnostics()
        }
        binding.codeEditor.subscribeEvent(PublishSearchResultEvent::class.java) { event, _ ->
            updateMatchCountLabel(event.searcher)
        }

        setUpSearchBar()
    }

    private fun setUpSearchBar() {
        binding.editSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = performSearch()
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.checkMatchCase.setOnCheckedChangeListener { _, _ -> performSearch() }
        binding.buttonSearchNext.setOnClickListener { binding.codeEditor.searcher.gotoNext() }
        binding.buttonSearchPrevious.setOnClickListener { binding.codeEditor.searcher.gotoPrevious() }
        binding.buttonCloseSearch.setOnClickListener { toggleSearchBar(false) }
        binding.buttonReplace.setOnClickListener {
            binding.codeEditor.searcher.replaceCurrentMatch(binding.editReplaceWith.text?.toString().orEmpty())
        }
        binding.buttonReplaceAll.setOnClickListener {
            binding.codeEditor.searcher.replaceAll(binding.editReplaceWith.text?.toString().orEmpty())
        }
    }

    private fun toggleSearchBar(show: Boolean) {
        binding.layoutSearchBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.editSearchQuery.requestFocus()
        } else {
            if (binding.codeEditor.searcher.hasQuery()) binding.codeEditor.searcher.stopSearch()
            binding.editSearchQuery.text?.clear()
            binding.editReplaceWith.text?.clear()
            binding.textMatchCount.text = ""
        }
    }

    private fun performSearch() {
        val query = binding.editSearchQuery.text?.toString().orEmpty()
        val searcher = binding.codeEditor.searcher
        if (query.isEmpty()) {
            if (searcher.hasQuery()) searcher.stopSearch()
            binding.textMatchCount.text = ""
            return
        }
        val caseInsensitive = !binding.checkMatchCase.isChecked
        searcher.search(query, EditorSearcher.SearchOptions(caseInsensitive, false))
    }

    private fun updateMatchCountLabel(searcher: EditorSearcher) {
        if (!searcher.hasQuery()) {
            binding.textMatchCount.text = ""
            return
        }
        val total = searcher.matchedPositionCount
        binding.textMatchCount.text = if (total == 0) {
            getString(R.string.msg_no_matches)
        } else {
            val current = searcher.currentMatchedPositionIndex
            "${if (current >= 0) current + 1 else 0}/$total"
        }
    }

    /**
     * Cross-file symbols (types/methods in other .java files of the project) only resolve once
     * they've been compiled to .class somewhere -- so compile the whole project once in the
     * background when the editor opens, seeding completion even before an explicit Build/Run.
     * This reflects the project as of the last successful compile, not live edits in other files.
     */
    private suspend fun seedProjectClassesForCompletion(projectDir: File, androidJar: File, language: SemanticJavaLanguage) {
        val classesDir = File(projectDir, "build/classes")
        // If a previous Build already generated R.java (Android app projects), include it so
        // code referencing R.id./R.layout./etc. can compile here too, same as the Build pipeline.
        val generatedSourcesDir = File(projectDir, "build/generated/r")
        val extraSourceDirs = if (generatedSourcesDir.isDirectory) listOf(generatedSourcesDir) else emptyList()
        withContext(Dispatchers.IO) {
            runCatching { JavaCompiler.compile(projectDir, classesDir, androidJar, extraSourceDirs) }
        }
        language.projectClassesDir = classesDir
        projectClassesDir = classesDir
        runDiagnostics()
    }

    private fun scheduleDiagnostics() {
        diagnosticsJob?.cancel()
        diagnosticsJob = lifecycleScope.launch {
            delay(DIAGNOSTICS_DEBOUNCE_MS)
            runDiagnostics()
        }
    }

    /** Resolves the current buffer with ECJ and shows its compile problems as inline markers. */
    private suspend fun runDiagnostics() {
        val androidJar = androidJarFile ?: return
        val text = binding.codeEditor.text.toString()
        val issues = withContext(Dispatchers.IO) {
            DiagnosticsEngine.analyze(androidJar, text, file.name, projectClassesDir)
        }
        val container = DiagnosticsContainer()
        container.addDiagnostics(
            issues.map { issue ->
                val severity = if (issue.severity == DiagnosticSeverity.ERROR) {
                    DiagnosticRegion.SEVERITY_ERROR
                } else {
                    DiagnosticRegion.SEVERITY_WARNING
                }
                DiagnosticRegion(issue.start, issue.end, severity, 0L, DiagnosticDetail(issue.message))
            }
        )
        binding.codeEditor.diagnostics = container
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
        when (item.itemId) {
            R.id.action_save -> {
                saveFile()
                return true
            }
            R.id.action_find -> {
                toggleSearchBar(binding.layoutSearchBar.visibility != View.VISIBLE)
                return true
            }
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
