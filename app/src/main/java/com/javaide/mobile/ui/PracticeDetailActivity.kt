package com.javaide.mobile.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.javaide.mobile.R
import com.javaide.mobile.compiler.AndroidJarProvider
import com.javaide.mobile.compiler.Dexer
import com.javaide.mobile.compiler.InterviewExercise
import com.javaide.mobile.compiler.JavaCompiler
import com.javaide.mobile.compiler.JavaRunner
import com.javaide.mobile.compiler.PracticeCategories
import com.javaide.mobile.databinding.ActivityPracticeDetailBinding
import io.github.rosemoe.sora.langs.java.JavaLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shows one interview-practice exercise: an editable buffer pre-filled with the known-correct
 * reference solution, a Run & Check action that compiles/dexes/runs it on-device and compares
 * the captured output against the exercise's expected output, and the expected output itself
 * for study. This is a worked-examples tool, not a blind quiz -- the solution is visible and
 * editable so it can be tweaked and re-run.
 */
class PracticeDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CLASS_NAME = "extra_class_name"
    }

    private lateinit var binding: ActivityPracticeDetailBinding
    private lateinit var exercise: InterviewExercise

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPracticeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val className = intent.getStringExtra(EXTRA_CLASS_NAME) ?: error("Missing $EXTRA_CLASS_NAME")
        exercise = PracticeCategories.find(className) ?: error("Unknown exercise: $className")
        title = PracticeCategories.displayTitle(exercise)

        binding.codeEditor.setEditorLanguage(JavaLanguage())
        binding.codeEditor.setText(exercise.source)
        binding.textExpectedOutput.text = exercise.expectedOutput

        binding.buttonRunCheck.setOnClickListener { runAndCheck() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun runAndCheck() {
        binding.buttonRunCheck.isEnabled = false
        binding.textResultStatus.visibility = View.GONE
        binding.textResultOutput.text = getString(R.string.running_message)

        val source = binding.codeEditor.text.toString()
        lifecycleScope.launch {
            val (passed, statusText, output) = withContext(Dispatchers.IO) {
                execute(source)
            }
            binding.textResultStatus.visibility = View.VISIBLE
            binding.textResultStatus.text = statusText
            binding.textResultStatus.setBackgroundColor(
                if (passed) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            )
            binding.textResultOutput.text = output
            binding.buttonRunCheck.isEnabled = true
        }
    }

    private fun execute(source: String): Triple<Boolean, String, String> {
        val androidJar = AndroidJarProvider.get(this)
        val workDir = File(cacheDir, "practice/${exercise.className}").apply {
            deleteRecursively()
            mkdirs()
        }
        val javaDir = File(workDir, "src/main/java").apply { mkdirs() }
        File(javaDir, "${exercise.className}.java").writeText(source)

        val classesDir = File(workDir, "classes")
        val compileResult = JavaCompiler.compile(workDir, classesDir, androidJar)
        if (!compileResult.success) {
            return Triple(false, getString(R.string.practice_failed), compileResult.log)
        }

        val dexDir = File(workDir, "dex")
        val dexResult = Dexer.dex(classesDir, dexDir, androidJar)
        if (!dexResult.success) {
            return Triple(false, getString(R.string.practice_failed), dexResult.log)
        }

        val optimizedDir = File(workDir, "dex-opt")
        val runResult = JavaRunner.run(classesDir, File(dexDir, "classes.dex"), optimizedDir)
        val passed = runResult.success && runResult.output.trim() == exercise.expectedOutput
        val status = getString(if (passed) R.string.practice_passed else R.string.practice_failed)
        return Triple(passed, status, runResult.output)
    }
}
