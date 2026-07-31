package com.javaide.mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.javaide.mobile.compiler.InterviewExercise
import com.javaide.mobile.compiler.PracticeCategories
import com.javaide.mobile.databinding.ItemPracticeExerciseBinding
import com.javaide.mobile.databinding.ItemPracticeHeaderBinding

private sealed class PracticeRow {
    data class Header(val title: String) : PracticeRow()
    data class Exercise(val exercise: InterviewExercise) : PracticeRow()
}

class PracticeAdapter(
    private val onExerciseClick: (InterviewExercise) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_EXERCISE = 1
    }

    private val rows: List<PracticeRow> = PracticeCategories.ALL.flatMap { category ->
        listOf(PracticeRow.Header(category.title)) + category.exercises.map { PracticeRow.Exercise(it) }
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is PracticeRow.Header -> VIEW_TYPE_HEADER
        is PracticeRow.Exercise -> VIEW_TYPE_EXERCISE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(ItemPracticeHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            ExerciseViewHolder(ItemPracticeExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is PracticeRow.Header -> (holder as HeaderViewHolder).bind(row.title)
            is PracticeRow.Exercise -> (holder as ExerciseViewHolder).bind(row.exercise)
        }
    }

    override fun getItemCount(): Int = rows.size

    private class HeaderViewHolder(private val binding: ItemPracticeHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.textCategoryHeader.text = title
        }
    }

    private inner class ExerciseViewHolder(private val binding: ItemPracticeExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(exercise: InterviewExercise) {
            binding.textExerciseTitle.text = PracticeCategories.displayTitle(exercise)
            binding.root.setOnClickListener { onExerciseClick(exercise) }
        }
    }
}
