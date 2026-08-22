package com.javaide.mobile.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlatViewTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var projectRoot: java.io.File

    @Before
    fun setUp() {
        // Create a project with files nested two levels deep
        val srcDir = tempFolder.newFolder("project", "src", "main", "java", "com", "example")
        java.io.File(srcDir, "Main.java").writeText("package com.example; public class Main {}")
        java.io.File(srcDir, "Helper.java").writeText("package com.example; public class Helper {}")
        projectRoot = tempFolder.root.resolve("project")
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun launchAtRoot(): ActivityScenario<FileExplorerActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), FileExplorerActivity::class.java).apply {
            putExtra(FileExplorerActivity.EXTRA_PROJECT_PATH, projectRoot.absolutePath)
            putExtra(FileExplorerActivity.EXTRA_PROJECT_NAME, "project")
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun flatViewShowsNestedFilesWithRelativePaths() {
        launchAtRoot().use {
            openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
            onView(withText("Flat view")).perform(click())

            // Both deeply nested files should be visible as relative paths — no drilling needed
            onView(withText("src/main/java/com/example/Main.java")).check(matches(isDisplayed()))
            onView(withText("src/main/java/com/example/Helper.java")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun flatViewFileOpensTapOpensEditor() {
        launchAtRoot().use {
            Intents.intending(hasComponent(EditorActivity::class.java.name))
                .respondWith(android.app.Instrumentation.ActivityResult(0, null))

            openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
            onView(withText("Flat view")).perform(click())

            onView(withText("src/main/java/com/example/Main.java")).perform(click())

            Intents.intended(
                allOf(
                    hasComponent(EditorActivity::class.java.name),
                    hasExtraWithKey(EditorActivity.EXTRA_FILE_PATH)
                )
            )
        }
    }

    @Test
    fun switchingBackToTreeViewRestoresDirectoryListing() {
        launchAtRoot().use {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext

            // Switch to flat view
            openActionBarOverflowOrOptionsMenu(ctx)
            onView(withText("Flat view")).perform(click())

            // Switch back to tree view
            openActionBarOverflowOrOptionsMenu(ctx)
            onView(withText("Tree view")).perform(click())

            // Root directory content (the src folder) should be visible again
            onView(withText("src")).check(matches(isDisplayed()))
        }
    }
}
