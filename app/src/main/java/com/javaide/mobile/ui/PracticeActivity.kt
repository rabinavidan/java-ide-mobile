package com.javaide.mobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.javaide.mobile.compiler.InterviewExercise
import com.javaide.mobile.databinding.ActivityPracticeBinding

/** Browsable list of the interview-practice exercises, grouped by category. */
class PracticeActivity : BaseActivity() {

    private lateinit var binding: ActivityPracticeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPracticeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerPractice.layoutManager = LinearLayoutManager(this)
        binding.recyclerPractice.adapter = PracticeAdapter(onExerciseClick = ::openExercise)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun openExercise(exercise: InterviewExercise) {
        val intent = Intent(this, PracticeDetailActivity::class.java)
        intent.putExtra(PracticeDetailActivity.EXTRA_CLASS_NAME, exercise.className)
        startActivity(intent)
    }
}
