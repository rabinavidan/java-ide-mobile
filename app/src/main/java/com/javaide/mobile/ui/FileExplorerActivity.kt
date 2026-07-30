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
import com.javaide.mobile.compiler.Dexer
import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.compiler.JavaRunner
import com.javaide.mobile.compiler.ManifestUtils
import com.javaide.mobile.compiler.Packager
import com.javaide.mobile.compiler.ResourceCompiler
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
        when (item.itemId) {
            R.id.action_build -> {
                runBuild()
                return true
            }
            R.id.action_run -> {
                runProject()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun runProject() {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle(R.string.running)
            .setMessage(R.string.running_message)
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            val androidJar = AndroidJarProvider.get(this@FileExplorerActivity)
            val projectDir = File(projectPath)
            val classesDir = File(projectDir, "build/classes")
            val dexDir = File(projectDir, "build/dex")
            val optimizedDexDir = File(projectDir, "build/dex-opt")

            val (success, log) = withContext(Dispatchers.IO) {
                val runLog = StringBuilder()

                val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar)
                runLog.appendLine("--- compile ---").appendLine(compileResult.log)
                if (!compileResult.success) {
                    return@withContext false to runLog.toString()
                }

                val dexResult = Dexer.dex(classesDir, dexDir, androidJar)
                runLog.appendLine().appendLine("--- dex ---").appendLine(dexResult.log)
                if (!dexResult.success) {
                    return@withContext false to runLog.toString()
                }

                val runResult = JavaRunner.run(classesDir, File(dexDir, "classes.dex"), optimizedDexDir)
                runLog.appendLine().appendLine("--- output ---").appendLine(runResult.output)

                runResult.success to runLog.toString()
            }

            progressDialog.dismiss()

            val intent = Intent(this@FileExplorerActivity, BuildOutputActivity::class.java)
            intent.putExtra(BuildOutputActivity.EXTRA_SUCCESS, success)
            intent.putExtra(BuildOutputActivity.EXTRA_LOG, log)
            intent.putExtra(BuildOutputActivity.EXTRA_TITLE_RES, R.string.title_run_output)
            intent.putExtra(BuildOutputActivity.EXTRA_SUCCESS_TEXT_RES, R.string.run_succeeded)
            intent.putExtra(BuildOutputActivity.EXTRA_FAILURE_TEXT_RES, R.string.run_failed)
            startActivity(intent)
        }
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
            val manifestFile = File(projectDir, "src/main/AndroidManifest.xml")
            val generatedSourcesDir = File(projectDir, "build/generated/r")
            val classesDir = File(projectDir, "build/classes")
            val dexDir = File(projectDir, "build/dex")
            val unsignedApk = File(projectDir, "build/outputs/unsigned.apk")
            val signedApk = File(projectDir, "build/outputs/app-debug.apk")

            val (success, log, packageName) = withContext(Dispatchers.IO) {
                val buildLog = StringBuilder()

                val resourceResult = ResourceCompiler.compile(projectDir, androidJar, generatedSourcesDir)
                buildLog.appendLine("--- resources ---").appendLine(resourceResult.log)
                if (!resourceResult.success || resourceResult.apkModule == null) {
                    return@withContext Triple(false, buildLog.toString(), null as String?)
                }

                val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar, listOf(generatedSourcesDir))
                buildLog.appendLine().appendLine("--- compile ---").appendLine(compileResult.log)
                if (!compileResult.success) {
                    return@withContext Triple(false, buildLog.toString(), null as String?)
                }

                val dexResult = Dexer.dex(classesDir, dexDir, androidJar)
                buildLog.appendLine().appendLine("--- dex ---").appendLine(dexResult.log)
                if (!dexResult.success) {
                    return@withContext Triple(false, buildLog.toString(), null as String?)
                }

                val packageResult = Packager.packageApk(
                    resourceResult.apkModule,
                    File(dexDir, "classes.dex"),
                    unsignedApk,
                    signedApk
                )
                buildLog.appendLine().appendLine("--- package ---").appendLine(packageResult.log)

                val packageName = if (packageResult.success) ManifestUtils.readPackageName(manifestFile) else null
                Triple(packageResult.success, buildLog.toString(), packageName)
            }

            progressDialog.dismiss()

            val intent = Intent(this@FileExplorerActivity, BuildOutputActivity::class.java)
            intent.putExtra(BuildOutputActivity.EXTRA_SUCCESS, success)
            intent.putExtra(BuildOutputActivity.EXTRA_LOG, log)
            if (success && packageName != null) {
                intent.putExtra(BuildOutputActivity.EXTRA_APK_PATH, signedApk.absolutePath)
                intent.putExtra(BuildOutputActivity.EXTRA_PACKAGE_NAME, packageName)
            }
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
