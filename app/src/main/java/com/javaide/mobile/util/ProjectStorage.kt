package com.javaide.mobile.util

import android.content.Context
import com.javaide.mobile.model.ProjectTemplate
import com.javaide.mobile.model.ProjectType
import java.io.File

object ProjectStorage {

    private const val PROJECTS_DIR = "projects"
    private val NAME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_]*$")

    fun projectsRoot(context: Context): File =
        File(context.filesDir, PROJECTS_DIR).apply { mkdirs() }

    fun listProjects(context: Context): List<File> =
        projectsRoot(context).listFiles { file -> file.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun isValidProjectName(name: String): Boolean = NAME_PATTERN.matches(name)

    fun projectExists(context: Context, name: String): Boolean =
        File(projectsRoot(context), name).exists()

    /** Creates a new project directory scaffolded from [ProjectTemplate]. */
    fun createProject(context: Context, name: String, type: ProjectType): File {
        val projectDir = File(projectsRoot(context), name)
        check(!projectDir.exists()) { "Project already exists: $name" }
        projectDir.mkdirs()
        val packageName = "com.example.${name.lowercase()}"
        ProjectTemplate.create(projectDir, name, packageName, type)
        return projectDir
    }

    /** Renames a project directory in place. Throws if [newName] is invalid or already taken. */
    fun renameProject(context: Context, project: File, newName: String): File {
        check(isValidProjectName(newName)) { "Invalid project name: $newName" }
        val target = File(projectsRoot(context), newName)
        check(!target.exists()) { "Project already exists: $newName" }
        check(project.renameTo(target)) { "Failed to rename ${project.name} to $newName" }
        return target
    }

    /** Deletes a project directory and everything under it. */
    fun deleteProject(project: File) {
        check(project.deleteRecursively()) { "Failed to delete ${project.name}" }
    }
}
