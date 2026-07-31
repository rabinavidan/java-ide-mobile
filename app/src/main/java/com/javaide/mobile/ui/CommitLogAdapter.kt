package com.javaide.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.javaide.mobile.databinding.ItemCommitBinding
import com.javaide.mobile.vcs.GitCommitInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommitLogAdapter : RecyclerView.Adapter<CommitLogAdapter.CommitViewHolder>() {

    private val commits = mutableListOf<GitCommitInfo>()
    private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.US)

    fun submitList(newCommits: List<GitCommitInfo>) {
        commits.clear()
        commits.addAll(newCommits)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommitViewHolder {
        val binding = ItemCommitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommitViewHolder, position: Int) {
        holder.bind(commits[position])
    }

    override fun getItemCount(): Int = commits.size

    inner class CommitViewHolder(private val binding: ItemCommitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(commit: GitCommitInfo) {
            binding.textCommitHash.text = commit.hash.take(8)
            binding.textCommitAuthor.text = "${commit.author} · ${dateFormat.format(Date(commit.time))}"
            binding.textCommitMessage.text = commit.message
        }
    }
}
