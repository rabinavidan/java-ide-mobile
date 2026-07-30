package com.javaide.mobile.model

import java.io.File

/**
 * Scaffolds a minimal Android project (manifest + one activity + resources)
 * that later phases (compiler/dexer/packager) will build into an APK.
 */
object ProjectTemplate {

    fun create(projectDir: File, projectName: String, packageName: String) {
        val javaDir = File(projectDir, "src/main/java/${packageName.replace('.', '/')}")
        val resValuesDir = File(projectDir, "src/main/res/values")
        val resLayoutDir = File(projectDir, "src/main/res/layout")
        listOf(javaDir, resValuesDir, resLayoutDir).forEach { it.mkdirs() }

        File(projectDir, "src/main/AndroidManifest.xml").writeText(manifest(packageName))
        File(javaDir, "MainActivity.java").writeText(mainActivity(packageName))
        File(resLayoutDir, "activity_main.xml").writeText(activityMainLayout())
        File(resValuesDir, "strings.xml").writeText(strings(projectName))
    }

    private fun manifest(packageName: String) = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
        |    package="$packageName">
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
