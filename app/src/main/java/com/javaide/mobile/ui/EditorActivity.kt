package com.javaide.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.javaide.mobile.R
import com.javaide.mobile.compiler.AndroidJarProvider
import com.javaide.mobile.compiler.Dexer
import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.compiler.JavaRunner
import com.javaide.mobile.compiler.LibJars
import com.javaide.mobile.completion.DefinitionFinder
import com.javaide.mobile.completion.DefinitionTarget
import com.javaide.mobile.completion.DiagnosticSeverity
import com.javaide.mobile.completion.DiagnosticsEngine
import com.javaide.mobile.completion.ImportIndex
import com.javaide.mobile.completion.ImportInserter
import com.javaide.mobile.completion.SemanticJavaLanguage
import com.javaide.mobile.data.AppDatabase
import com.javaide.mobile.data.EditorState
import com.javaide.mobile.data.Logger
import com.javaide.mobile.databinding.ActivityEditorBinding
import com.javaide.mobile.util.ClassRenamer
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.SelectionMovement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/** One open editor tab: which file, its buffer (cached while inactive), and its cursor position. */
data class EditorTab(val file: File, var text: String, var cursorLine: Int = 0, var cursorColumn: Int = 0)

/**
 * A single instance is reused across every file opened for the same project (`singleTask`
 * launch mode + [onNewIntent]) so files can be kept open as tabs instead of piling up separate
 * Activities on the back stack. Only one [io.github.rosemoe.sora.widget.CodeEditor] backs all
 * tabs -- sora-editor has no public way to swap in a different Content object -- so switching
 * tabs caches/restores plain text + cursor position per tab rather than a live Content per tab;
 * the tradeoff is that undo/redo history resets when you leave a tab and come back (auto-save
 * still protects the actual text).
 */
class EditorActivity : BaseActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_PROJECT_PATH = "extra_project_path"
        /** 0-based line to scroll/select to, e.g. when opened from a Find in Project result. */
        const val EXTRA_JUMP_LINE = "extra_jump_line"
        /** Query to pre-fill in the find bar and highlight, alongside [EXTRA_JUMP_LINE]. */
        const val EXTRA_JUMP_QUERY = "extra_jump_query"
        private const val AUTO_SAVE_DEBOUNCE_MS = 1500L
        private const val DIAGNOSTICS_DEBOUNCE_MS = 1500L
    }

    private lateinit var binding: ActivityEditorBinding
    private lateinit var tabAdapter: EditorTabAdapter
    private val tabs = mutableListOf<EditorTab>()
    private var activeTabIndex = -1
    private var currentProjectPath: String? = null
    private var autoSaveJob: Job? = null
    private var diagnosticsJob: Job? = null
    private var androidJarFile: File? = null
    private var projectClassesDir: File? = null

    private val activeTab: EditorTab
        get() = tabs[activeTabIndex]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setUpTabStrip()

        binding.codeEditor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            scheduleAutoSave()
            scheduleDiagnostics()
        }
        binding.codeEditor.subscribeEvent(PublishSearchResultEvent::class.java) { event, _ ->
            updateMatchCountLabel(event.getSearcher())
        }

        setUpSearchBar()
        setUpAccessoryBar()
        setUpConsole()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val path = intent.getStringExtra(EXTRA_FILE_PATH) ?: error("Missing $EXTRA_FILE_PATH")
        val projectPath = intent.getStringExtra(EXTRA_PROJECT_PATH)
        val isNewTab = openOrSwitchToFile(path, projectPath)

        val jumpLine = intent.getIntExtra(EXTRA_JUMP_LINE, -1)
        if (jumpLine >= 0) {
            jumpToSearchResult(jumpLine, intent.getStringExtra(EXTRA_JUMP_QUERY).orEmpty())
        } else if (isNewTab) {
            restoreCursorPosition(activeTab)
        }
    }

    private fun setUpTabStrip() {
        tabAdapter = EditorTabAdapter(
            onTabClick = { index ->
                if (index != activeTabIndex) {
                    flushActiveTab()
                    switchToTab(index)
                }
            },
            onTabClose = { index -> closeTab(index) }
        )
        binding.recyclerTabs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerTabs.adapter = tabAdapter

        // Deliberately does NOT finish() this Activity: EditorActivity is singleTask, so tapping
        // a file in the explorer that pops up here reuses this same alive instance via
        // onNewIntent() and adds a tab, instead of losing every open tab to the back button.
        binding.buttonOpenFile.setOnClickListener {
            val projectPath = currentProjectPath ?: return@setOnClickListener
            val intent = Intent(this, FileExplorerActivity::class.java)
            intent.putExtra(FileExplorerActivity.EXTRA_PROJECT_PATH, projectPath)
            intent.putExtra(FileExplorerActivity.EXTRA_PROJECT_NAME, File(projectPath).name)
            startActivity(intent)
        }
    }

    private fun renderTabStrip() {
        tabAdapter.submitTabs(tabs, activeTabIndex)
    }

    /**
     * Opens [filePath] as a tab -- switching to it if already open, otherwise creating a new one
     * -- and returns whether this was a brand-new tab (as opposed to switching to one already
     * open). Opening a file from a *different* project than the one currently loaded drops all
     * existing tabs first: this instance is scoped to one project's tabs at a time.
     */
    private fun openOrSwitchToFile(filePath: String, projectPath: String?): Boolean {
        if (projectPath != null && currentProjectPath != null && projectPath != currentProjectPath) {
            flushActiveTab()
            tabs.clear()
            activeTabIndex = -1
            androidJarFile = null
            projectClassesDir = null
        } else {
            flushActiveTab()
        }
        if (currentProjectPath == null) currentProjectPath = projectPath

        val targetPath = File(filePath).absolutePath
        val existingIndex = tabs.indexOfFirst { it.file.absolutePath == targetPath }
        return if (existingIndex >= 0) {
            switchToTab(existingIndex)
            false
        } else {
            val file = File(filePath)
            tabs.add(EditorTab(file = file, text = file.readText()))
            switchToTab(tabs.size - 1)
            Logger.info(this, "editor", "Opened: ${file.name}")
            true
        }
    }

    private fun switchToTab(index: Int) {
        activeTabIndex = index
        val tab = tabs[index]
        title = tab.file.name
        binding.codeEditor.setText(tab.text)
        runCatching { binding.codeEditor.setSelection(tab.cursorLine, tab.cursorColumn) }
        configureLanguageForActiveTab()
        renderTabStrip()
    }

    private fun closeTab(index: Int) {
        val wasActive = index == activeTabIndex
        if (wasActive) flushActiveTab()
        Logger.info(this, "editor", "Closed: ${tabs[index].file.name}")
        tabs.removeAt(index)
        if (tabs.isEmpty()) {
            finish()
            return
        }
        when {
            wasActive -> switchToTab((index - 1).coerceIn(0, tabs.size - 1))
            index < activeTabIndex -> {
                activeTabIndex -= 1
                renderTabStrip()
            }
            else -> renderTabStrip()
        }
    }

    /** Third-party .jar dependencies for the current project (see LibJars), recomputed fresh each
     * time since a library can be added mid-session via the file explorer's "Add library" action. */
    private fun currentLibJars(): List<File> =
        currentProjectPath?.let { LibJars.jarsIn(File(it)) } ?: emptyList()

    /** Sets up syntax highlighting/completion/diagnostics for whichever tab is now active. */
    private fun configureLanguageForActiveTab() {
        val tab = activeTab
        if (tab.file.extension != "java") {
            binding.codeEditor.setEditorLanguage(EmptyLanguage())
            return
        }
        binding.codeEditor.setEditorLanguage(JavaLanguage())
        lifecycleScope.launch {
            val androidJar = androidJarFile ?: withContext(Dispatchers.IO) {
                AndroidJarProvider.get(this@EditorActivity)
            }.also { androidJarFile = it }
            // The user may have switched tabs again before this finished loading.
            if (activeTab !== tab) return@launch

            val language = SemanticJavaLanguage(androidJar).apply {
                fileName = tab.file.name
                projectClassesDir = this@EditorActivity.projectClassesDir
                libJars = currentLibJars()
            }
            binding.codeEditor.setEditorLanguage(language)
            runDiagnostics()

            val projectPath = currentProjectPath
            if (projectClassesDir == null && projectPath != null) {
                seedProjectClassesForCompletion(File(projectPath), androidJar, language)
            }
        }
    }

    /** Quick-insert bar for characters/actions that are awkward to reach on a soft keyboard. */
    private fun setUpAccessoryBar() {
        binding.symbolInputView.bindEditor(binding.codeEditor)
        binding.symbolInputView.addSymbols(
            arrayOf("Tab", "{", "}", "(", ")", ";", "\"", "'", "<", ">", "=", ".", ",", "_"),
            arrayOf("\t", "{", "}", "(", ")", ";", "\"", "'", "<", ">", "=", ".", ",", "_")
        )

        binding.buttonUndo.setOnClickListener { if (binding.codeEditor.canUndo()) binding.codeEditor.undo() }
        binding.buttonRedo.setOnClickListener { if (binding.codeEditor.canRedo()) binding.codeEditor.redo() }
        binding.buttonMoveLeft.setOnClickListener { binding.codeEditor.moveSelection(SelectionMovement.LEFT) }
        binding.buttonMoveRight.setOnClickListener { binding.codeEditor.moveSelection(SelectionMovement.RIGHT) }
        binding.buttonMoveUp.setOnClickListener { binding.codeEditor.moveSelection(SelectionMovement.UP) }
        binding.buttonMoveDown.setOnClickListener { binding.codeEditor.moveSelection(SelectionMovement.DOWN) }
    }

    private fun setUpConsole() {
        binding.buttonCloseConsole.setOnClickListener {
            binding.layoutConsole.visibility = View.GONE
        }
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
     * background the first time this instance needs it, seeding completion even before an
     * explicit Build/Run. This reflects the project as of the last successful compile, not live
     * edits in other files, and is shared across every tab of the same project.
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
            DiagnosticsEngine.analyze(androidJar, text, activeTab.file.name, projectClassesDir, currentLibJars())
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
        captureActiveTabState()
        tabs.getOrNull(activeTabIndex)?.let { tab -> lifecycleScope.launch { persistTabToDisk(tab) } }
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
            R.id.action_run -> {
                saveFile()
                runProject()
                return true
            }
            R.id.action_save -> {
                saveFile()
                return true
            }
            R.id.action_find -> {
                toggleSearchBar(binding.layoutSearchBar.visibility != View.VISIBLE)
                return true
            }
            R.id.action_fix_imports -> {
                fixImports()
                return true
            }
            R.id.action_go_to_definition -> {
                goToDefinition()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showConsole(text: String) {
        binding.textConsoleOutput.text = text
        binding.layoutConsole.visibility = View.VISIBLE
        binding.scrollConsole.post { binding.scrollConsole.fullScroll(View.FOCUS_DOWN) }
    }

    private fun runProject() {
        val projectPath = currentProjectPath ?: run {
            Toast.makeText(this, "No project associated with this file", Toast.LENGTH_SHORT).show()
            return
        }

        showConsole(getString(R.string.running_message))

        lifecycleScope.launch {
            val androidJar = AndroidJarProvider.get(this@EditorActivity)
            val projectDir = File(projectPath)
            val classesDir = File(projectDir, "build/classes")
            val dexDir = File(projectDir, "build/dex")
            val optimizedDexDir = File(projectDir, "build/dex-opt")
            val libJars = LibJars.jarsIn(projectDir)

            val (success, log) = withContext(Dispatchers.IO) {
                val runLog = StringBuilder()
                val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar, libJars = libJars)
                runLog.appendLine("--- compile ---").appendLine(compileResult.log)
                if (!compileResult.success) return@withContext false to runLog.toString()

                val dexResult = Dexer.dex(classesDir, dexDir, androidJar, libJars)
                runLog.appendLine().appendLine("--- dex ---").appendLine(dexResult.log)
                if (!dexResult.success) return@withContext false to runLog.toString()

                val runResult = JavaRunner.run(classesDir, File(dexDir, "classes.dex"), optimizedDexDir)
                runLog.appendLine().appendLine("--- output ---").appendLine(runResult.output)
                runResult.success to runLog.toString()
            }

            val projectName = File(projectPath).name
            if (success) Logger.info(this@EditorActivity, "run", "Run succeeded for '$projectName'")
            else Logger.warn(this@EditorActivity, "run", "Run failed for '$projectName'")

            showConsole(log)
        }
    }

    /**
     * Resolves the symbol at the cursor with DefinitionFinder and jumps to it: an exact offset
     * within this same file, or -- for a type declared elsewhere in the project -- switches to
     * that file (opening it as a new tab if it wasn't already) and scrolls to its declaration line.
     */
    private fun goToDefinition() {
        val androidJar = androidJarFile ?: return
        val text = binding.codeEditor.text.toString()
        val cursor = binding.codeEditor.cursor.left
        lifecycleScope.launch {
            val target = withContext(Dispatchers.IO) {
                DefinitionFinder.find(androidJar, text, cursor, activeTab.file.name, projectClassesDir, currentLibJars())
            }
            when (target) {
                is DefinitionTarget.SameFile -> {
                    val position = binding.codeEditor.text.indexer.getCharPosition(target.offset)
                    runCatching { binding.codeEditor.setSelection(position.line, position.column) }
                }
                is DefinitionTarget.OtherProjectFile -> {
                    val projectPath = currentProjectPath
                    val location = projectPath?.let { path ->
                        withContext(Dispatchers.IO) { ClassRenamer.findDeclaration(File(path), target.simpleName) }
                    }
                    if (location != null) {
                        openOrSwitchToFile(location.file.absolutePath, projectPath)
                        runCatching { binding.codeEditor.setSelection(location.lineNumber, 0) }
                    } else {
                        Toast.makeText(this@EditorActivity, R.string.msg_no_definition_found, Toast.LENGTH_SHORT).show()
                    }
                }
                null -> Toast.makeText(this@EditorActivity, R.string.msg_no_definition_found, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Scans the current buffer for ECJ's "X cannot be resolved to a type" errors, resolves each
     * unresolved simple name against [ImportIndex] (android.jar's own class list), and inserts
     * whichever imports it can -- prompting when a name has more than one candidate package.
     */
    private fun fixImports() {
        val androidJar = androidJarFile ?: return
        lifecycleScope.launch {
            val issues = withContext(Dispatchers.IO) {
                DiagnosticsEngine.analyze(androidJar, binding.codeEditor.text.toString(), activeTab.file.name, projectClassesDir, currentLibJars())
            }
            val unresolvedNames = issues.mapNotNull { it.unresolvedTypeName }.distinct()

            val resolved = mutableListOf<String>()
            for (name in unresolvedNames) {
                val candidates = withContext(Dispatchers.IO) { ImportIndex.candidatesFor(androidJar, name) }
                when {
                    candidates.isEmpty() -> Unit
                    candidates.size == 1 -> resolved += candidates[0]
                    else -> chooseImport(name, candidates)?.let { resolved += it }
                }
            }

            val toInsert = ImportInserter.pendingImports(binding.codeEditor.text.toString(), resolved)
            if (toInsert.isEmpty()) {
                Toast.makeText(this@EditorActivity, R.string.msg_no_import_fixes, Toast.LENGTH_SHORT).show()
                return@launch
            }

            val insertLine = ImportInserter.insertionLine(binding.codeEditor.text.toString())
            val importBlock = toInsert.joinToString("") { "import $it;\n" }
            binding.codeEditor.text.insert(insertLine, 0, importBlock)

            Toast.makeText(
                this@EditorActivity,
                getString(R.string.msg_imports_added, toInsert.size),
                Toast.LENGTH_SHORT
            ).show()
            runDiagnostics()
        }
    }

    private suspend fun chooseImport(simpleName: String, candidates: List<String>): String? =
        suspendCancellableCoroutine { cont ->
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_choose_import_title, simpleName))
                .setItems(candidates.toTypedArray()) { _, which -> cont.resume(candidates[which]) }
                .setOnCancelListener { cont.resume(null) }
                .show()
        }

    private fun saveFile() {
        val tab = activeTab
        val text = binding.codeEditor.text.toString()
        runCatching { tab.file.writeText(text) }
            .onSuccess {
                tab.text = text
                Toast.makeText(this, R.string.file_saved, Toast.LENGTH_SHORT).show()
            }
            .onFailure { Toast.makeText(this, R.string.file_save_failed, Toast.LENGTH_SHORT).show() }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = lifecycleScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            captureActiveTabState()
            persistTabToDisk(tabs[activeTabIndex])
        }
    }

    /** Copies the live buffer/cursor into the active tab's model -- synchronous, no I/O. */
    private fun captureActiveTabState() {
        if (activeTabIndex !in tabs.indices) return
        val tab = tabs[activeTabIndex]
        tab.text = binding.codeEditor.text.toString()
        tab.cursorLine = binding.codeEditor.cursor.leftLine
        tab.cursorColumn = binding.codeEditor.cursor.leftColumn
    }

    /** Writes a tab's already-captured state to disk + Room. Safe to call after switching away. */
    private suspend fun persistTabToDisk(tab: EditorTab) {
        withContext(Dispatchers.IO) {
            runCatching {
                tab.file.writeText(tab.text)
                Logger.info(this@EditorActivity, "editor", "Saved: ${tab.file.name}")
            }
            runCatching {
                AppDatabase.get(this@EditorActivity).dao().upsertEditorState(
                    EditorState(
                        filePath = tab.file.absolutePath,
                        cursorLine = tab.cursorLine,
                        cursorColumn = tab.cursorColumn,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /** Captures + flushes whichever tab is currently active, e.g. before switching away from it. */
    private fun flushActiveTab() {
        if (activeTabIndex !in tabs.indices) return
        autoSaveJob?.cancel()
        captureActiveTabState()
        val tab = tabs[activeTabIndex]
        lifecycleScope.launch { persistTabToDisk(tab) }
    }

    /** Opened from a Find in Project result: scroll to the line and highlight the query. */
    private fun jumpToSearchResult(line: Int, query: String) {
        runCatching { binding.codeEditor.setSelection(line, 0) }
        if (query.isNotEmpty()) {
            toggleSearchBar(true)
            binding.editSearchQuery.setText(query)
        }
    }

    /** Only meaningful for a brand-new tab: an already-open tab already has its live cursor. */
    private fun restoreCursorPosition(tab: EditorTab) {
        lifecycleScope.launch {
            val state = withContext(Dispatchers.IO) {
                AppDatabase.get(this@EditorActivity).dao().getEditorState(tab.file.absolutePath)
            } ?: return@launch
            if (activeTab !== tab) return@launch
            tab.cursorLine = state.cursorLine
            tab.cursorColumn = state.cursorColumn
            runCatching { binding.codeEditor.setSelection(state.cursorLine, state.cursorColumn) }
        }
    }
}
