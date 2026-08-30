package com.javaide.mobile.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.javaide.mobile.R
import org.hamcrest.Matchers.allOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditorTabsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var projectDir: File
    private lateinit var mainFile: File
    private lateinit var helperFile: File

    @Before
    fun setUp() {
        projectDir = tempFolder.newFolder("project")
        val srcDir = File(projectDir, "src/main/java/com/example").apply { mkdirs() }
        mainFile = File(srcDir, "Main.java").apply {
            writeText("package com.example; public class Main { public static void main(String[] a) {} }")
        }
        helperFile = File(srcDir, "Helper.java").apply {
            writeText("package com.example; public class Helper {}")
        }
    }

    @Test
    fun openingFileShowsTabWithFileName() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, mainFile.absolutePath)
            putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
        }

        ActivityScenario.launch<EditorActivity>(intent).use {
            onView(allOf(withId(R.id.textTabName), withText("Main.java"))).check(matches(isDisplayed()))
        }
    }

    @Test
    fun openingSecondFileAddsSecondTab() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(ctx, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, mainFile.absolutePath)
            putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        ActivityScenario.launch<EditorActivity>(intent).use { scenario ->
            onView(allOf(withId(R.id.textTabName), withText("Main.java"))).check(matches(isDisplayed()))

            val intent2 = Intent(ctx, EditorActivity::class.java).apply {
                putExtra(EditorActivity.EXTRA_FILE_PATH, helperFile.absolutePath)
                putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            scenario.onActivity { activity ->
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, intent2)
            }

            onView(allOf(withId(R.id.textTabName), withText("Main.java"))).check(matches(isDisplayed()))
            onView(allOf(withId(R.id.textTabName), withText("Helper.java"))).check(matches(isDisplayed()))
        }
    }

    @Test
    fun openingSameFileTwiceDoesNotCreateDuplicateTab() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(ctx, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, mainFile.absolutePath)
            putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        ActivityScenario.launch<EditorActivity>(intent).use { scenario ->
            // Open the same file again via onNewIntent
            scenario.onActivity { activity ->
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, intent)
            }

            // Still shows exactly one "Main.java" tab — not two
            onView(allOf(withId(R.id.textTabName), withText("Main.java"))).check(matches(isDisplayed()))
        }
    }
}
