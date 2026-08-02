package com.javaide.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.javaide.mobile.R
import com.javaide.mobile.databinding.ActivityProjectSearchBinding
import com.javaide.mobile.util.ProjectSearch
import com.javaide.mobile.util.SearchMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Recursive text search across every file in a project, with tap-to-jump into the editor. */
class ProjectSearchActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROJECT_PATH = "extra_project_path"
    }

    private lateinit var binding: ActivityProjectSearchBinding
    private lateinit var projectDir: File
    private lateinit var adapter: ProjectSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        projectDir = File(intent.getStringExtra(EXTRA_PROJECT_PATH) ?: error("Missing $EXTRA_PROJECT_PATH"))

        adapter = ProjectSearchAdapter(onResultClick = ::openMatch)
        binding.recyclerResults.layoutManager = LinearLayoutManager(this)
        binding.recyclerResults.adapter = adapter

        binding.buttonSearch.setOnClickListener { runSearch() }
        binding.checkMatchCase.setOnCheckedChangeListener { _, _ -> runSearch() }
        binding.editSearchQuery.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                runSearch()
                true
            } else {
                false
            }
        }
        binding.buttonReplaceAll.setOnClickListener { confirmReplaceAll() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun runSearch() {
        val query = binding.editSearchQuery.text?.toString().orEmpty()
        if (query.isEmpty()) return
        val caseSensitive = binding.checkMatchCase.isChecked

        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                ProjectSearch.search(projectDir, query, caseSensitive)
            }
            adapter.submitResults(projectDir, results)
            binding.textEmptyResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /**
     * Re-runs the search fresh (so the confirmation shows an up-to-date file count, in case the
     * query changed since the last search) before asking to confirm -- this rewrites file
     * contents across the whole project in one irreversible step, unlike a single-file replace.
     */
    private fun confirmReplaceAll() {
        val query = binding.editSearchQuery.text?.toString().orEmpty()
        if (query.isEmpty()) {
            Toast.makeText(this, R.string.error_replace_query_required, Toast.LENGTH_SHORT).show()
            return
        }
        val replacement = binding.editReplaceWith.text?.toString().orEmpty()
        val caseSensitive = binding.checkMatchCase.isChecked

        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) { ProjectSearch.search(projectDir, query, caseSensitive) }
            adapter.submitResults(projectDir, results)
            binding.textEmptyResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            if (results.isEmpty()) return@launch

            AlertDialog.Builder(this@ProjectSearchActivity)
                .setTitle(R.string.dialog_replace_all_title)
                .setMessage(getString(R.string.dialog_replace_all_message, query, replacement, results.size))
                .setPositiveButton(R.string.action_replace_all) { _, _ -> performReplaceAll(query, replacement, caseSensitive) }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }
    }

    private fun performReplaceAll(query: String, replacement: String, caseSensitive: Boolean) {
        lifecycleScope.launch {
            val changedFiles = withContext(Dispatchers.IO) {
                ProjectSearch.replaceAll(projectDir, query, replacement, caseSensitive)
            }
            Toast.makeText(this@ProjectSearchActivity, getString(R.string.msg_replace_all_done, changedFiles), Toast.LENGTH_SHORT).show()
            runSearch()
        }
    }

    private fun openMatch(match: SearchMatch) {
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra(EditorActivity.EXTRA_FILE_PATH, match.file.absolutePath)
        intent.putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
        intent.putExtra(EditorActivity.EXTRA_JUMP_LINE, match.lineNumber - 1)
        intent.putExtra(EditorActivity.EXTRA_JUMP_QUERY, binding.editSearchQuery.text?.toString().orEmpty())
        startActivity(intent)
    }
}
