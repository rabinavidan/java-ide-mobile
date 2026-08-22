package com.javaide.mobile.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.javaide.mobile.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

/**
 * Verifies the Run button in EditorActivity shows the inline console panel.
 * Does not assert specific program output — compilation on-device depends on
 * ECJ and android.jar extraction which are covered by InterviewExercisesRunTest.
 * This test covers the UI flow: Run tapped → console visible → close button hides it.
 */
@RunWith(AndroidJUnit4::class)
class RunFromEditorConsoleTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mainFile: File
    private lateinit var projectDir: File

    @Before
    fun setUp() {
        projectDir = tempFolder.newFolder("project")
        val srcDir = File(projectDir, "src/main/java/com/example").apply { mkdirs() }
        mainFile = File(srcDir, "Main.java").apply {
            writeText(
                "package com.example;\n" +
                "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello from console!\");\n" +
                "    }\n" +
                "}\n"
            )
        }
    }

    @Test
    fun runButtonShowsConsolePanelImmediately() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, mainFile.absolutePath)
            putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
        }

        ActivityScenario.launch<EditorActivity>(intent).use {
            // Console is hidden initially
            onView(withId(R.id.layoutConsole)).check(matches(androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility(
                androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
            )))

            // Tap Run
            onView(withId(R.id.action_run)).perform(click())

            // Console appears immediately (shows "Compiling and running…" while pipeline runs)
            waitUntilVisible(R.id.layoutConsole)
            onView(withId(R.id.layoutConsole)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun closeButtonHidesConsolePanel() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_FILE_PATH, mainFile.absolutePath)
            putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
        }

        ActivityScenario.launch<EditorActivity>(intent).use {
            onView(withId(R.id.action_run)).perform(click())
            waitUntilVisible(R.id.layoutConsole)

            onView(withId(R.id.buttonCloseConsole)).perform(click())

            onView(withId(R.id.layoutConsole)).check(matches(
                androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility(
                    androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
                )
            ))
        }
    }

    private fun waitUntilVisible(viewId: Int, timeoutMs: Long = 10_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (e: AssertionError) {
                Thread.sleep(300)
            }
        }
        onView(withId(viewId)).check(matches(isDisplayed()))
    }
}
