package com.javaide.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

    private fun openMatch(match: SearchMatch) {
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra(EditorActivity.EXTRA_FILE_PATH, match.file.absolutePath)
        intent.putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
        intent.putExtra(EditorActivity.EXTRA_JUMP_LINE, match.lineNumber - 1)
        intent.putExtra(EditorActivity.EXTRA_JUMP_QUERY, binding.editSearchQuery.text?.toString().orEmpty())
        startActivity(intent)
    }
}
