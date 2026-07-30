plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
        getByName("main") {
            assets.srcDir(androidJarAssetDir)
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

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
