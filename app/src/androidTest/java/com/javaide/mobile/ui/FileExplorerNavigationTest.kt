package com.javaide.mobile.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * Regression test for the singleTask back-navigation bug:
 * tapping a file from a nested subdirectory must open EditorActivity,
 * not collapse the back stack back to the parent directory.
 */
@RunWith(AndroidJUnit4::class)
class FileExplorerNavigationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var subDir: java.io.File
    private lateinit var mainFile: java.io.File

    @Before
    fun setUp() {
        // Simulate a project subdirectory with a Java file, matching the real layout
        // src/main/java/com/example/ that users navigate into before clicking a file.
        subDir = tempFolder.newFolder("project", "src", "main", "java", "com", "example")
        mainFile = java.io.File(subDir, "Main.java").apply {
            writeText("package com.example;\npublic class Main { public static void main(String[] a) {} }")
        }
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun tappingFileInSubdirectoryOpensEditor() {
        val projectRoot = tempFolder.root.resolve("project").absolutePath

        val intent = Intent(ApplicationProvider.getApplicationContext(), FileExplorerActivity::class.java).apply {
            putExtra(FileExplorerActivity.EXTRA_PROJECT_PATH, projectRoot)
            putExtra(FileExplorerActivity.EXTRA_DIR_PATH, subDir.absolutePath)
            putExtra(FileExplorerActivity.EXTRA_PROJECT_NAME, "example")
        }

        ActivityScenario.launch<FileExplorerActivity>(intent).use {
            // Stub EditorActivity so it doesn't actually need to render
            Intents.intending(hasComponent(EditorActivity::class.java.name))
                .respondWith(android.app.Instrumentation.ActivityResult(0, null))

            onView(withText(containsString("Main.java"))).perform(click())

            // Verify EditorActivity was launched with the correct file — not that we went back
            Intents.intended(
                allOf(
                    hasComponent(EditorActivity::class.java.name),
                    hasExtraWithKey(EditorActivity.EXTRA_FILE_PATH)
                )
            )
        }
    }
}
