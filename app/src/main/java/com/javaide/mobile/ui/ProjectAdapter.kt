package com.javaide.mobile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.javaide.mobile.R
import com.javaide.mobile.databinding.ItemProjectBinding
import java.io.File

class ProjectAdapter(
    private val onProjectClick: (File) -> Unit,
    private val onRenameClick: (File) -> Unit,
    private val onDeleteClick: (File) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    private val projects = mutableListOf<File>()

    fun submitList(newProjects: List<File>) {
        projects.clear()
        projects.addAll(newProjects)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(projects[position])
    }

    override fun getItemCount(): Int = projects.size

    inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: File) {
            binding.textProjectName.text = project.name
            binding.textProjectPath.text = project.absolutePath
            binding.root.setOnClickListener { onProjectClick(project) }
            binding.buttonProjectMenu.setOnClickListener { showMenu(it, project) }
        }

        private fun showMenu(anchor: View, project: File) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menuInflater.inflate(R.menu.menu_project_item, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_rename_project -> {
                        onRenameClick(project)
                        true
                    }
                    R.id.action_delete_project -> {
                        onDeleteClick(project)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}
