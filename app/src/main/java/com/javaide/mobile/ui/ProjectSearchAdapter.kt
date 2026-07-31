package com.javaide.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.javaide.mobile.databinding.ItemSearchHeaderBinding
import com.javaide.mobile.databinding.ItemSearchResultBinding
import com.javaide.mobile.util.SearchFileResult
import com.javaide.mobile.util.SearchMatch
import java.io.File

private sealed class SearchRow {
    data class Header(val relativePath: String) : SearchRow()
    data class Result(val match: SearchMatch) : SearchRow()
}

class ProjectSearchAdapter(
    private val onResultClick: (SearchMatch) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_RESULT = 1
    }

    private var rows: List<SearchRow> = emptyList()

    fun submitResults(projectDir: File, results: List<SearchFileResult>) {
        rows = results.flatMap { fileResult ->
            val relativePath = fileResult.file.relativeTo(projectDir).path
            listOf(SearchRow.Header(relativePath)) + fileResult.matches.map { SearchRow.Result(it) }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is SearchRow.Header -> VIEW_TYPE_HEADER
        is SearchRow.Result -> VIEW_TYPE_RESULT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(ItemSearchHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            ResultViewHolder(ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is SearchRow.Header -> (holder as HeaderViewHolder).bind(row.relativePath)
            is SearchRow.Result -> (holder as ResultViewHolder).bind(row.match)
        }
    }

    override fun getItemCount(): Int = rows.size

    private class HeaderViewHolder(private val binding: ItemSearchHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(relativePath: String) {
            binding.textFileHeader.text = relativePath
        }
    }

    private inner class ResultViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(match: SearchMatch) {
            binding.textLineNumber.text = "L${match.lineNumber}"
            binding.textLineContent.text = match.lineText
            binding.root.setOnClickListener { onResultClick(match) }
        }
    }
}
