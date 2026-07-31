package com.javaide.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.javaide.mobile.R
import com.javaide.mobile.data.Logger
import com.javaide.mobile.databinding.ActivityMainBinding
import com.javaide.mobile.databinding.DialogNewProjectBinding
import com.javaide.mobile.model.ProjectType
import com.javaide.mobile.util.ProjectStorage
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = ProjectAdapter(onProjectClick = ::openProject)
        binding.recyclerProjects.layoutManager = LinearLayoutManager(this)
        binding.recyclerProjects.adapter = adapter

        binding.fabNewProject.setOnClickListener { showNewProjectDialog() }

        refreshProjects()
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
        if (item.itemId == R.id.action_history) {
            startActivity(Intent(this, HistoryActivity::class.java))
            return true
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
}
