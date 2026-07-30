package com.javaide.mobile.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.javaide.mobile.R
import com.javaide.mobile.compiler.AndroidJarProvider
import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.databinding.ActivityFileExplorerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Browses the contents of a single project directory, one folder level at a time. */
class FileExplorerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROJECT_PATH = "extra_project_path"
        const val EXTRA_PROJECT_NAME = "extra_project_name"
        const val EXTRA_DIR_PATH = "extra_dir_path"
    }

    private lateinit var binding: ActivityFileExplorerBinding
    private lateinit var adapter: FileAdapter
    private lateinit var currentDir: File
    private lateinit var projectPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileExplorerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        projectPath = intent.getStringExtra(EXTRA_PROJECT_PATH)
            ?: error("Missing $EXTRA_PROJECT_PATH")
        val dirPath = intent.getStringExtra(EXTRA_DIR_PATH) ?: projectPath
        currentDir = File(dirPath)

        title = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: currentDir.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = FileAdapter(onFileClick = ::onEntryClick)
        binding.recyclerFiles.layoutManager = LinearLayoutManager(this)
        binding.recyclerFiles.adapter = adapter

        loadEntries()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_file_explorer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_build) {
            runBuild()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun runBuild() {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle(R.string.building)
            .setMessage(R.string.building_message)
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            val androidJar = AndroidJarProvider.get(this@FileExplorerActivity)
            val projectDir = File(projectPath)
            val outputDir = File(projectDir, "build/classes")

            val result = withContext(Dispatchers.IO) {
                JavaCompiler.compile(projectDir, outputDir, androidJar)
            }

            progressDialog.dismiss()

            val intent = Intent(this@FileExplorerActivity, BuildOutputActivity::class.java)
            intent.putExtra(BuildOutputActivity.EXTRA_SUCCESS, result.success)
            intent.putExtra(BuildOutputActivity.EXTRA_LOG, result.log)
            startActivity(intent)
        }
    }

    private fun loadEntries() {
        val entries = currentDir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
        adapter.submitList(entries)
        binding.textEmptyFiles.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onEntryClick(entry: File) {
        if (entry.isDirectory) {
            val intent = Intent(this, FileExplorerActivity::class.java)
            intent.putExtra(EXTRA_PROJECT_PATH, projectPath)
            intent.putExtra(EXTRA_DIR_PATH, entry.absolutePath)
            intent.putExtra(EXTRA_PROJECT_NAME, entry.name)
            startActivity(intent)
        } else {
            val intent = Intent(this, EditorActivity::class.java)
            intent.putExtra(EditorActivity.EXTRA_FILE_PATH, entry.absolutePath)
            startActivity(intent)
        }
    }
}
