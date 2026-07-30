package com.javaide.mobile.model

import java.io.File

enum class ProjectType {
    /** A full Android app: manifest + activity + resources, buildable into an installable APK. */
    ANDROID_APP,

    /** A single Java class with public static void main, run in-process (no manifest/resources). */
    JAVA_CONSOLE
}

/**
 * Scaffolds either a minimal Android app (manifest + one activity + resources, buildable
 * into an APK) or a plain Java class for quick practice (compiled + dexed + run in-process,
 * no manifest/resources needed).
 */
object ProjectTemplate {

    fun create(projectDir: File, projectName: String, packageName: String, type: ProjectType) {
        when (type) {
            ProjectType.ANDROID_APP -> createAndroidApp(projectDir, projectName, packageName)
            ProjectType.JAVA_CONSOLE -> createConsoleProject(projectDir, packageName)
        }
    }

    private fun createAndroidApp(projectDir: File, projectName: String, packageName: String) {
        val javaDir = File(projectDir, "src/main/java/${packageName.replace('.', '/')}")
        val resValuesDir = File(projectDir, "src/main/res/values")
        val resLayoutDir = File(projectDir, "src/main/res/layout")
        listOf(javaDir, resValuesDir, resLayoutDir).forEach { it.mkdirs() }

        File(projectDir, "src/main/AndroidManifest.xml").writeText(manifest(packageName))
        File(javaDir, "MainActivity.java").writeText(mainActivity(packageName))
        File(resLayoutDir, "activity_main.xml").writeText(activityMainLayout())
        File(resValuesDir, "strings.xml").writeText(strings(projectName))
    }

    private fun createConsoleProject(projectDir: File, packageName: String) {
        val javaDir = File(projectDir, "src/main/java/${packageName.replace('.', '/')}").apply { mkdirs() }
        File(javaDir, "Main.java").writeText(mainClass(packageName))
    }

    private fun mainClass(packageName: String) = """
        |package $packageName;
        |
        |public class Main {
        |    public static void main(String[] args) {
        |        System.out.println("Hello, Java!");
        |    }
        |}
        |""".trimMargin()

    private fun manifest(packageName: String) = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
        |    package="$packageName">
        |
        |    <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="34" />
        |
        |    <application
        |        android:allowBackup="true"
        |        android:label="@string/app_name">
        |        <activity android:name=".MainActivity" android:exported="true">
        |            <intent-filter>
        |                <action android:name="android.intent.action.MAIN" />
        |                <category android:name="android.intent.category.LAUNCHER" />
        |            </intent-filter>
        |        </activity>
        |    </application>
        |</manifest>
        |""".trimMargin()

    private fun mainActivity(packageName: String) = """
        |package $packageName;
        |
        |import android.app.Activity;
        |import android.os.Bundle;
        |
        |public class MainActivity extends Activity {
        |    @Override
        |    protected void onCreate(Bundle savedInstanceState) {
        |        super.onCreate(savedInstanceState);
        |        setContentView(R.layout.activity_main);
        |    }
        |}
        |""".trimMargin()

    private fun activityMainLayout() = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        |    android:layout_width="match_parent"
        |    android:layout_height="match_parent"
        |    android:gravity="center"
        |    android:orientation="vertical">
        |
        |    <TextView
        |        android:layout_width="wrap_content"
        |        android:layout_height="wrap_content"
        |        android:text="@string/app_name" />
        |
        |</LinearLayout>
        |""".trimMargin()

    private fun strings(projectName: String) = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<resources>
        |    <string name="app_name">$projectName</string>
        |</resources>
        |""".trimMargin()
}
