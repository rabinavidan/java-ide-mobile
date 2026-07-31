package com.javaide.mobile.ui

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.javaide.mobile.databinding.ItemEditorTabBinding

class EditorTabAdapter(
    private val onTabClick: (Int) -> Unit,
    private val onTabClose: (Int) -> Unit
) : RecyclerView.Adapter<EditorTabAdapter.TabViewHolder>() {

    private var tabs: List<EditorTab> = emptyList()
    private var activeIndex: Int = -1

    fun submitTabs(newTabs: List<EditorTab>, newActiveIndex: Int) {
        tabs = newTabs.toList()
        activeIndex = newActiveIndex
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder =
        TabViewHolder(ItemEditorTabBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(tabs[position], position == activeIndex, position)
    }

    override fun getItemCount(): Int = tabs.size

    inner class TabViewHolder(private val binding: ItemEditorTabBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tab: EditorTab, isActive: Boolean, position: Int) {
            binding.textTabName.text = tab.file.name
            binding.textTabName.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
            binding.root.alpha = if (isActive) 1f else 0.6f
            binding.root.setOnClickListener { onTabClick(position) }
            binding.buttonCloseTab.setOnClickListener { onTabClose(position) }
        }
    }
}
