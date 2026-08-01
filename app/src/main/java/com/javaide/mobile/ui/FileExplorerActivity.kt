package com.javaide.mobile.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.javaide.mobile.R
import com.javaide.mobile.compiler.AndroidJarProvider
import com.javaide.mobile.compiler.Dexer
import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.compiler.JavaRunner
import com.javaide.mobile.compiler.LibJars
import com.javaide.mobile.compiler.ManifestUtils
import com.javaide.mobile.compiler.Packager
import com.javaide.mobile.compiler.ResourceCompiler
import com.javaide.mobile.data.AppDatabase
import com.javaide.mobile.data.Logger
import com.javaide.mobile.databinding.ActivityFileExplorerBinding
import com.javaide.mobile.util.ClassRenamer
import com.javaide.mobile.util.FileOps
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

    private val addLibraryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importLibraryJar(it) }
    }

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

        adapter = FileAdapter(
            onFileClick = ::onEntryClick,
            onRenameClick = ::showRenameEntryDialog,
            onDeleteClick = ::confirmDeleteEntry
        )
        binding.recyclerFiles.layoutManager = LinearLayoutManager(this)
        binding.recyclerFiles.adapter = adapter

        binding.fabNewEntry.setOnClickListener { showNewEntryMenu(it) }

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
            R.id.action_version_control -> {
                val intent = Intent(this, VersionControlActivity::class.java)
                intent.putExtra(VersionControlActivity.EXTRA_PROJECT_PATH, projectPath)
                startActivity(intent)
                return true
            }
            R.id.action_find_in_project -> {
                val intent = Intent(this, ProjectSearchActivity::class.java)
                intent.putExtra(ProjectSearchActivity.EXTRA_PROJECT_PATH, projectPath)
                startActivity(intent)
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
            val libJars = LibJars.jarsIn(projectDir)

            val (success, log) = withContext(Dispatchers.IO) {
                val runLog = StringBuilder()

                val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar, libJars = libJars)
                runLog.appendLine("--- compile ---").appendLine(compileResult.log)
                if (!compileResult.success) {
                    return@withContext false to runLog.toString()
                }

                val dexResult = Dexer.dex(classesDir, dexDir, androidJar, libJars)
                runLog.appendLine().appendLine("--- dex ---").appendLine(dexResult.log)
                if (!dexResult.success) {
                    return@withContext false to runLog.toString()
                }

                val runResult = JavaRunner.run(classesDir, File(dexDir, "classes.dex"), optimizedDexDir)
                runLog.appendLine().appendLine("--- output ---").appendLine(runResult.output)

                runResult.success to runLog.toString()
            }

            progressDialog.dismiss()

            val projectName = File(projectPath).name
            if (success) {
                Logger.info(this@FileExplorerActivity, "run", "Run succeeded for '$projectName'")
            } else {
                Logger.warn(this@FileExplorerActivity, "run", "Run failed for '$projectName'")
            }

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
            val libJars = LibJars.jarsIn(projectDir)

            val (success, log, packageName) = withContext(Dispatchers.IO) {
                val buildLog = StringBuilder()

                val resourceResult = ResourceCompiler.compile(projectDir, androidJar, generatedSourcesDir)
                buildLog.appendLine("--- resources ---").appendLine(resourceResult.log)
                if (!resourceResult.success || resourceResult.apkModule == null) {
                    return@withContext Triple(false, buildLog.toString(), null as String?)
                }

                val compileResult = JavaCompiler.compile(projectDir, classesDir, androidJar, listOf(generatedSourcesDir), libJars)
                buildLog.appendLine().appendLine("--- compile ---").appendLine(compileResult.log)
                if (!compileResult.success) {
                    return@withContext Triple(false, buildLog.toString(), null as String?)
                }

                val dexResult = Dexer.dex(classesDir, dexDir, androidJar, libJars)
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

            val projectName = File(projectPath).name
            if (success) {
                Logger.info(this@FileExplorerActivity, "build", "Build succeeded for '$projectName'")
            } else {
                Logger.warn(this@FileExplorerActivity, "build", "Build failed for '$projectName'")
            }

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
            intent.putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectPath)
            startActivity(intent)
        }
    }

    private fun showNewEntryMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_new_entry, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_new_file -> {
                    showNewFileDialog()
                    true
                }
                R.id.action_new_folder -> {
                    showNewFolderDialog()
                    true
                }
                R.id.action_add_library -> {
                    addLibraryLauncher.launch(arrayOf("*/*"))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * Copies a picked .jar into <project>/libs/, the convention JavaCompiler/Dexer/
     * DiagnosticsEngine/SemanticCompletionEngine all pick dependency jars up from automatically.
     * Uses a generic "*/*" MIME filter for the picker since content providers don't reliably
     * report a specific MIME type for .jar files, validating the extension after the pick instead.
     */
    private fun importLibraryJar(uri: Uri) {
        val displayName = queryDisplayName(uri) ?: "library.jar"
        if (!displayName.endsWith(".jar")) {
            Toast.makeText(this, R.string.error_library_not_jar, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val libsDir = File(projectPath, "libs").apply { mkdirs() }
                    val target = File(libsDir, displayName)
                    val input = contentResolver.openInputStream(uri) ?: error("Could not open selected file")
                    input.use { stream -> target.outputStream().use { output -> stream.copyTo(output) } }
                    target
                }
            }
            result
                .onSuccess { target ->
                    Logger.info(this@FileExplorerActivity, "library", "Added library '${target.name}'")
                    Toast.makeText(
                        this@FileExplorerActivity,
                        getString(R.string.msg_library_added, target.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadEntries()
                }
                .onFailure {
                    Logger.error(this@FileExplorerActivity, "library", "Failed to add library: ${it.message}")
                    Toast.makeText(this@FileExplorerActivity, R.string.error_library_add_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun queryDisplayName(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }

    private fun showNewFileDialog() {
        val input = EditText(this).apply { hint = getString(R.string.hint_file_name) }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_new_file_title)
            .setView(input)
            .setPositiveButton(R.string.dialog_create) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                createEntry(name) { parent -> FileOps.createFile(File(projectPath), parent, name) }
                    ?.let { onEntryClick(it) }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showNewFolderDialog() {
        val input = EditText(this).apply { hint = getString(R.string.hint_folder_name) }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_new_folder_title)
            .setView(input)
            .setPositiveButton(R.string.dialog_create) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                createEntry(name) { parent -> FileOps.createFolder(parent, name) }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /** Validates [name], creates the entry via [create], refreshes the list, and returns it on success. */
    private fun createEntry(name: String, create: (File) -> File): File? {
        if (name.isEmpty() || !FileOps.isValidFileName(name)) {
            Toast.makeText(this, R.string.error_file_name_required, Toast.LENGTH_SHORT).show()
            return null
        }
        if (File(currentDir, name).exists()) {
            Toast.makeText(this, R.string.error_file_exists, Toast.LENGTH_SHORT).show()
            return null
        }
        val created = runCatching { create(currentDir) }
            .onFailure { Logger.error(this, "file", "Failed to create '$name': ${it.message}") }
            .onFailure { Toast.makeText(this, R.string.error_file_create_failed, Toast.LENGTH_SHORT).show() }
            .getOrNull()
        if (created != null) {
            Logger.info(this, "file", "Created '${created.name}'")
            loadEntries()
        }
        return created
    }

    private fun showRenameEntryDialog(entry: File) {
        val input = EditText(this).apply { setText(entry.name) }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_rename_file_title)
            .setView(input)
            .setPositiveButton(R.string.action_rename) { _, _ ->
                val newName = input.text?.toString()?.trim().orEmpty()
                renameEntry(entry, newName)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun renameEntry(entry: File, newName: String) {
        if (newName == entry.name) return
        if (newName.isEmpty() || !FileOps.isValidFileName(newName)) {
            Toast.makeText(this, R.string.error_file_name_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (File(entry.parentFile, newName).exists()) {
            Toast.makeText(this, R.string.error_file_exists, Toast.LENGTH_SHORT).show()
            return
        }
        val oldPath = entry.absolutePath
        // Only a candidate for a project-wide class rename if both sides are .java files --
        // renaming to/from a non-.java name is just a plain file rename.
        val oldClassName = entry.nameWithoutExtension.takeIf { entry.extension == "java" && newName.endsWith(".java") }
        val newClassName = newName.removeSuffix(".java").takeIf { oldClassName != null }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { FileOps.renameEntry(entry, newName) } }
            result
                .onSuccess { renamed ->
                    withContext(Dispatchers.IO) {
                        AppDatabase.get(this@FileExplorerActivity).dao().deleteEditorStatesUnderPath(oldPath)
                    }
                    val message = if (oldClassName != null && newClassName != null &&
                        ClassRenamer.isClassDeclaration(renamed, oldClassName)
                    ) {
                        val changedFiles = withContext(Dispatchers.IO) {
                            ClassRenamer.renameAcrossProject(File(projectPath), oldClassName, newClassName)
                        }
                        getString(R.string.msg_class_renamed, oldClassName, newClassName, changedFiles)
                    } else {
                        getString(R.string.msg_file_renamed)
                    }
                    Logger.info(this@FileExplorerActivity, "file", "Renamed '${entry.name}' to '$newName'")
                    Toast.makeText(this@FileExplorerActivity, message, Toast.LENGTH_LONG).show()
                    loadEntries()
                }
                .onFailure {
                    Logger.error(this@FileExplorerActivity, "file", "Failed to rename '${entry.name}': ${it.message}")
                    Toast.makeText(this@FileExplorerActivity, R.string.error_file_rename_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun confirmDeleteEntry(entry: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_file_title)
            .setMessage(getString(R.string.dialog_delete_file_message, entry.name))
            .setPositiveButton(R.string.action_delete) { _, _ -> deleteEntry(entry) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun deleteEntry(entry: File) {
        val path = entry.absolutePath
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { FileOps.deleteEntry(entry) } }
            result
                .onSuccess {
                    withContext(Dispatchers.IO) {
                        AppDatabase.get(this@FileExplorerActivity).dao().deleteEditorStatesUnderPath(path)
                    }
                    Logger.info(this@FileExplorerActivity, "file", "Deleted '${entry.name}'")
                    Toast.makeText(this@FileExplorerActivity, R.string.msg_file_deleted, Toast.LENGTH_SHORT).show()
                    loadEntries()
                }
                .onFailure {
                    Logger.error(this@FileExplorerActivity, "file", "Failed to delete '${entry.name}': ${it.message}")
                    Toast.makeText(this@FileExplorerActivity, R.string.error_file_delete_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }
}
