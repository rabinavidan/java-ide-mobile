package com.javaide.mobile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.javaide.mobile.R
import com.javaide.mobile.databinding.ItemFileBinding
import java.io.File

class FileAdapter(
    private val onFileClick: (File) -> Unit,
    private val onRenameClick: (File) -> Unit,
    private val onDeleteClick: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private val entries = mutableListOf<File>()

    fun submitList(newEntries: List<File>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    inner class FileViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: File) {
            binding.textFileName.text = entry.name
            binding.iconFileType.setImageResource(
                if (entry.isDirectory) android.R.drawable.ic_menu_agenda
                else android.R.drawable.ic_menu_edit
            )
            binding.root.setOnClickListener { onFileClick(entry) }
            binding.buttonFileMenu.setOnClickListener { showMenu(it, entry) }
        }

        private fun showMenu(anchor: View, entry: File) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menuInflater.inflate(R.menu.menu_entry_actions, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_rename_entry -> {
                        onRenameClick(entry)
                        true
                    }
                    R.id.action_delete_entry -> {
                        onDeleteClick(entry)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}
