package com.javaide.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.javaide.mobile.data.EventEntry
import com.javaide.mobile.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val entries = mutableListOf<EventEntry>()
    private val dateFormat = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)

    fun submitList(newEntries: List<EventEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: EventEntry) {
            binding.textCategory.text = "[${entry.level}] ${entry.category}"
            binding.textTimestamp.text = dateFormat.format(Date(entry.timestamp))
            binding.textMessage.text = entry.message
        }
    }
}
