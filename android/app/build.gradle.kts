plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun localProperty(name: String): String {
    val localProperties = rootProject.file("local.properties")
    if (!localProperties.exists()) return ""

    return localProperties.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || !trimmed.contains("=")) null
            else trimmed.substringBefore("=").trim() to trimmed.substringAfter("=").trim()
        }
        .firstOrNull { it.first == name }
        ?.second
        .orEmpty()
}

android {
    namespace = "com.invoke.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.invoke.android"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }

        buildConfigField("String", "PRIVY_APP_ID", "\"${localProperty("privy.app.id")}\"")
        buildConfigField("String", "PRIVY_APP_CLIENT_ID", "\"${localProperty("privy.app.client.id")}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")

    // Networking (Ollama + Composio)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Auth (Privy user auth only; Supabase remains database/backend storage)
    implementation("io.privy:privy-core:0.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // sherpa-onnx for local STT (pre-built AAR)
    implementation(files("libs/sherpa-onnx.aar"))

    testImplementation("junit:junit:4.13.2")
}
