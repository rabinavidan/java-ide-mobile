plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

// ECJ needs android.jar on its compile classpath to compile Android source on-device.
// Rather than committing a ~26MB binary to the repo, copy the same android.jar this
// module is already compiled against (android.bootClasspath) into assets at build time.
val androidJarAssetDir = layout.buildDirectory.dir("generated/androidJarAsset")

android {
    namespace = "com.javaide.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.javaide.mobile"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*", "module-info.class")
        }
    }

    sourceSets {
        // Exercise fixtures used to live only in test/androidTest; now the in-app Practice screen
        // needs them too, so they're compiled once as part of main and test/androidTest pick them
        // up transitively (their classpaths already include the tested app's main classes) --
        // adding the dir to all three would compile InterviewExercises.kt twice and collide as a
        // duplicate class when the androidTest APK is dexed against the app under test.
        getByName("main") {
            assets.srcDir(androidJarAssetDir)
            java.srcDir("src/testShared/java")
        }
    }
}

val copyAndroidJar by tasks.registering(Copy::class) {
    from(provider { android.bootClasspath.first() })
    into(androidJarAssetDir)
    rename { "android.jar" }
}

tasks.matching { it.name.matches(Regex("merge.*Assets")) }.configureEach {
    dependsOn(copyAndroidJar)
}

// --- Allure reporting for local unit tests ---
// allure-junit4 (the AllureJunit4 RunListener) needs allure-junit4-aspect to actually get
// registered: that module carries an AspectJ aspect (META-INF/aop.xml) that hooks
// "new RunNotifier()" and calls notifier.addListener(new AllureJunit4()) right after
// construction, regardless of which JUnit runner created it (Gradle's own, plain
// JUnitCore, an IDE, ...). That, plus the aspectjweaver javaagent and a results
// directory, is all that's needed - no custom test runner required. Verified standalone:
// ran a throwaway JUnit4 test with this exact wiring and confirmed real result JSON
// files (with correct pass/fail status and stack traces) were produced.
// Scoped to local unit tests only; instrumented (on-device) Allure reporting would need
// a custom AndroidJUnitRunner plus pulling results off the device, out of scope here.
val aspectjWeaver: Configuration by configurations.creating
val allureCli: Configuration by configurations.creating

val allureResultsDir = layout.buildDirectory.dir("allure-results")
val allureCliDir = layout.buildDirectory.dir("allure-cli")

// Local unit tests can't use AndroidJarProvider (needs a Context), so hand them the same
// android.jar this module is compiled against via a system property. Also wires up Allure.
tasks.withType<Test>().configureEach {
    systemProperty("android.jar.path", provider { android.bootClasspath.first().absolutePath }.get())
    systemProperty("allure.results.directory", allureResultsDir.get().asFile.absolutePath)
    jvmArgs("-javaagent:${aspectjWeaver.asPath}")
}

val extractAllureCli by tasks.registering(Copy::class) {
    from(allureCli.map { zipTree(it) })
    into(allureCliDir)
    doLast {
        file(allureCliDir.get().file("allure-${libs.versions.allure.get()}/bin/allure")).setExecutable(true)
    }
}

val allureReport by tasks.registering(Exec::class) {
    dependsOn(extractAllureCli, "testDebugUnitTest")
    workingDir(allureCliDir)
    commandLine(
        "allure-${libs.versions.allure.get()}/bin/allure", "generate",
        allureResultsDir.get().asFile.absolutePath,
        "-o", layout.buildDirectory.dir("reports/allure-report").get().asFile.absolutePath,
        "--clean"
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity)

    implementation(libs.sora.editor)
    implementation(libs.sora.language.java)
    implementation(libs.ecj)
    implementation(libs.r8)
    implementation(libs.arscLib)
    implementation(libs.apksig)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.jgit)
    implementation(libs.jgit.gpg.bc)
    implementation(libs.bouncycastle.pg)
    implementation(libs.bouncycastle.prov)

    testImplementation(libs.junit)
    testImplementation(libs.allure.junit4)
    testImplementation(libs.allure.junit4.aspect)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    aspectjWeaver(libs.aspectjweaver)
    allureCli(libs.allure.commandline) {
        isTransitive = false
        artifact {
            type = "zip"
        }
    }
}
