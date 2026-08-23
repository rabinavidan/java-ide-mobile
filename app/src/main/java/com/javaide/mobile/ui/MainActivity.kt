package com.javaide.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.javaide.mobile.R
import com.javaide.mobile.data.AppDatabase
import com.javaide.mobile.data.Logger
import com.javaide.mobile.databinding.ActivityMainBinding
import com.javaide.mobile.databinding.DialogNewProjectBinding
import com.javaide.mobile.model.ProjectType
import com.javaide.mobile.util.ProjectStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : BaseActivity() {

    private companion object {
        const val PREFS_NAME = "onboarding"
        const val PREF_HAS_SEEN_WELCOME = "has_seen_welcome"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = ProjectAdapter(
            onProjectClick = ::openProject,
            onRenameClick = ::showRenameProjectDialog,
            onDeleteClick = ::confirmDeleteProject
        )
        binding.recyclerProjects.layoutManager = LinearLayoutManager(this)
        binding.recyclerProjects.adapter = adapter

        binding.fabNewProject.setOnClickListener { showNewProjectDialog() }

        refreshProjects()
        showWelcomeDialogIfFirstLaunch()
    }

    /** Shown once, ever, on the very first launch -- points to the always-available Help screen. */
    private fun showWelcomeDialogIfFirstLaunch() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(PREF_HAS_SEEN_WELCOME, false)) return

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_welcome_title)
            .setMessage(R.string.dialog_welcome_message)
            .setPositiveButton(R.string.dialog_got_it, null)
            .setOnDismissListener { prefs.edit().putBoolean(PREF_HAS_SEEN_WELCOME, true).apply() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        refreshProjects()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                return true
            }
            R.id.action_practice -> {
                startActivity(Intent(this, PracticeActivity::class.java))
                return true
            }
            R.id.action_help -> {
                startActivity(Intent(this, HelpActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshProjects() {
        val projects = ProjectStorage.listProjects(this)
        adapter.submitList(projects)
        binding.textEmpty.visibility = if (projects.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun openProject(project: File) {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.putExtra(FileExplorerActivity.EXTRA_PROJECT_PATH, project.absolutePath)
        intent.putExtra(FileExplorerActivity.EXTRA_PROJECT_NAME, project.name)
        startActivity(intent)
    }

    private fun showNewProjectDialog() {
        val dialogBinding = DialogNewProjectBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_new_project_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_create, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.editProjectName.text?.toString()?.trim().orEmpty()
                when {
                    name.isEmpty() || !ProjectStorage.isValidProjectName(name) -> {
                        dialogBinding.editProjectName.error = getString(R.string.error_project_name_required)
                    }
                    ProjectStorage.projectExists(this, name) -> {
                        dialogBinding.editProjectName.error = getString(R.string.error_project_exists)
                    }
                    else -> {
                        val type = if (dialogBinding.radioAndroidApp.isChecked) {
                            ProjectType.ANDROID_APP
                        } else {
                            ProjectType.JAVA_CONSOLE
                        }
                        runCatching { ProjectStorage.createProject(this, name, type) }
                            .onSuccess {
                                Logger.info(this, "project", "Created project '$name' ($type)")
                                refreshProjects()
                                dialog.dismiss()
                            }
                            .onFailure {
                                Logger.error(this, "project", "Failed to create project '$name': ${it.message}")
                                dialogBinding.editProjectName.error = getString(R.string.error_project_create_failed)
                            }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showRenameProjectDialog(project: File) {
        val input = EditText(this).apply { setText(project.name) }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_rename_project_title)
            .setView(input)
            .setPositiveButton(R.string.action_rename, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = input.text?.toString()?.trim().orEmpty()
                when {
                    newName == project.name -> dialog.dismiss()
                    newName.isEmpty() || !ProjectStorage.isValidProjectName(newName) -> {
                        input.error = getString(R.string.error_project_name_required)
                    }
                    ProjectStorage.projectExists(this, newName) -> {
                        input.error = getString(R.string.error_project_exists)
                    }
                    else -> {
                        val oldPath = project.absolutePath
                        lifecycleScope.launch {
                            val renamed = withContext(Dispatchers.IO) {
                                runCatching { ProjectStorage.renameProject(this@MainActivity, project, newName) }
                            }
                            renamed
                                .onSuccess {
                                    withContext(Dispatchers.IO) {
                                        AppDatabase.get(this@MainActivity).dao().deleteEditorStatesUnderPath(oldPath)
                                    }
                                    Logger.info(this@MainActivity, "project", "Renamed project '${project.name}' to '$newName'")
                                    Toast.makeText(this@MainActivity, R.string.msg_project_renamed, Toast.LENGTH_SHORT).show()
                                    refreshProjects()
                                    dialog.dismiss()
                                }
                                .onFailure {
                                    Logger.error(this@MainActivity, "project", "Failed to rename '${project.name}' to '$newName': ${it.message}")
                                    input.error = getString(R.string.error_project_rename_failed)
                                }
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteProject(project: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_project_title)
            .setMessage(getString(R.string.dialog_delete_project_message, project.name))
            .setPositiveButton(R.string.action_delete) { _, _ -> deleteProject(project) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun deleteProject(project: File) {
        val path = project.absolutePath
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { ProjectStorage.deleteProject(project) } }
            result
                .onSuccess {
                    withContext(Dispatchers.IO) {
                        AppDatabase.get(this@MainActivity).dao().deleteEditorStatesUnderPath(path)
                    }
                    Logger.info(this@MainActivity, "project", "Deleted project '${project.name}'")
                    Toast.makeText(this@MainActivity, R.string.msg_project_deleted, Toast.LENGTH_SHORT).show()
                    refreshProjects()
                }
                .onFailure {
                    Logger.error(this@MainActivity, "project", "Failed to delete '${project.name}': ${it.message}")
                    Toast.makeText(this@MainActivity, R.string.error_project_delete_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }
}
