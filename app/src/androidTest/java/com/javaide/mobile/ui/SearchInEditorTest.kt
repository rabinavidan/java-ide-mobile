package com.javaide.mobile.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.javaide.mobile.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SearchInEditorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mainFile: File
    private lateinit var projectDir: File

    @Before
    fun setUp() {
        projectDir = tempFolder.newFolder("project")
        val srcDir = File(projectDir, "src/main/java").apply { mkdirs() }
        mainFile = File(srcDir, "Main.java").apply {
            writeText(
                "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"hello world\");\n" +
                "    }\n" +
                "}\n"
            )
        }
    }

    private fun launchEditor(): ActivityScenario<EditorActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, mainFile.absolutePath)
            putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun findBarHiddenByDefault() {
        launchEditor().use {
            onView(withId(R.id.layoutSearchBar)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test
    fun findMenuItemShowsFindBar() {
        launchEditor().use {
            onView(withId(R.id.action_find)).perform(click())

            onView(withId(R.id.layoutSearchBar)).check(matches(isDisplayed()))
            onView(withId(R.id.editSearchQuery)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun closeFindBarHidesSearchBar() {
        launchEditor().use {
            onView(withId(R.id.action_find)).perform(click())
            onView(withId(R.id.layoutSearchBar)).check(matches(isDisplayed()))

            onView(withId(R.id.buttonCloseSearch)).perform(click())

            onView(withId(R.id.layoutSearchBar)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test
    fun typingQueryInFindBarDoesNotCrash() {
        launchEditor().use {
            onView(withId(R.id.action_find)).perform(click())
            onView(withId(R.id.editSearchQuery)).perform(typeText("hello"))

            onView(withId(R.id.layoutSearchBar)).check(matches(isDisplayed()))
        }
    }
}
