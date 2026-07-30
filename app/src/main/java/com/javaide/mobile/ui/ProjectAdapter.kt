package com.javaide.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.javaide.mobile.databinding.ItemProjectBinding
import java.io.File

class ProjectAdapter(
    private val onProjectClick: (File) -> Unit
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
        }
    }
}
