import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

fun String.asBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

fun gitOutput(vararg args: String): String {
    return gitOutputOrNull(*args) ?: "unknown"
}

fun gitOutputOrNull(vararg args: String): String? {
    return try {
        val output = providers.exec {
            commandLine("git", *args)
            isIgnoreExitValue = true
        }
        val result = output.result.get()
        if (result.exitValue == 0) {
            output.standardOutput.asText.get().trim()
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

fun gitDirty(): Boolean {
    return gitOutputOrNull("status", "--porcelain").orEmpty().isNotBlank()
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.dealplanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dealplanner"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val geminiApiKey = localProperties.getProperty("gemini.api.key")
            ?: System.getenv("GEMINI_API_KEY")
            ?: ""
        val geminiModel = localProperties.getProperty("gemini.model")
            ?: System.getenv("GEMINI_MODEL")
            ?: "gemini-3.5-flash"

        buildConfigField("String", "GEMINI_API_KEY", geminiApiKey.asBuildConfigString())
        buildConfigField("String", "GEMINI_MODEL", geminiModel.asBuildConfigString())
        buildConfigField("String", "GIT_BRANCH", gitOutput("rev-parse", "--abbrev-ref", "HEAD").asBuildConfigString())
        buildConfigField("String", "GIT_SHA", gitOutput("rev-parse", "--short", "HEAD").asBuildConfigString())
        buildConfigField("boolean", "GIT_DIRTY", gitDirty().toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.matching { task ->
    task.name.startsWith("generate") && task.name.endsWith("BuildConfig")
}.configureEach {
    inputs.property("dealPlannerGitBranch", gitOutput("rev-parse", "--abbrev-ref", "HEAD"))
    inputs.property("dealPlannerGitSha", gitOutput("rev-parse", "--short", "HEAD"))
    inputs.property("dealPlannerGitDirty", gitDirty())
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ML Kit Text Recognition
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // ZXing for barcode
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // CameraX for image capture
    val cameraxVersion = "1.3.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("com.google.truth:truth:1.1.5")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
