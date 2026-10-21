import java.util.Properties
import org.gradle.api.tasks.Exec

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.ntbbloodbath.pocketnumbat"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "io.github.ntbbloodbath.pocketnumbat"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val androidSdkDir = localProperties.getProperty("sdk.dir")
    ?: System.getenv("ANDROID_HOME")
    ?: throw GradleException("Set sdk.dir in local.properties or ANDROID_HOME")

val androidNdkDir = localProperties.getProperty("ndk.dir")
    ?: System.getenv("ANDROID_NDK_HOME")
    ?: throw GradleException("Set ndk.dir in local.properties or ANDROID_NDK_HOME")

val rustJniLibsDir = layout.buildDirectory.dir("rustJniLibs")

val buildRust = tasks.register<Exec>("buildRust") {
    group = "rust"
    description = "Cross-compiles the Numbat bridge for Android"

    workingDir = rootProject.file("rust")
    commandLine(
        "cargo", "ndk",
        "--platform", "26",
        "-t", "arm64-v8a",
        "-t", "x86_64",
        "-o", rustJniLibsDir.get().asFile.absolutePath,
        "build", "--release",
    )

    environment("ANDROID_HOME", androidSdkDir)
    environment("ANDROID_NDK_HOME", androidNdkDir)
    environment("ANDROID_NDK_ROOT", androidNdkDir)

    inputs.dir(rootProject.file("rust/src"))
    inputs.file(rootProject.file("rust/Cargo.toml"))
    inputs.file(rootProject.file("rust/Cargo.lock"))
    outputs.dir(rustJniLibsDir)
}

tasks.named("preBuild").configure {
    dependsOn(buildRust)
}

android.sourceSets["main"].jniLibs.srcDir(rustJniLibsDir.get().asFile)

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
