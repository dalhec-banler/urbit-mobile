plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.nativeplanet.urbit.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.nativeplanet.urbit.launcher"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // Ship endpoints. On the phone the ship is local (Eyre :80, loopback :12321).
        // For emulator development pass -PshipUrl=http://10.0.2.2:8080 -PloopbackUrl=http://10.0.2.2:12322
        // (10.0.2.2 is the host as seen from the emulator).
        val shipUrl = (project.findProperty("shipUrl") as String?) ?: "http://127.0.0.1:80"
        val loopbackUrl = (project.findProperty("loopbackUrl") as String?) ?: "http://127.0.0.1:12321"
        buildConfigField("String", "SHIP_URL", "\"$shipUrl\"")
        buildConfigField("String", "LOOPBACK_URL", "\"$loopbackUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Networking (for ship HTTP API)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.json:json:20231013")

    // WebView for Urbit app frontends
    implementation("androidx.webkit:webkit:1.9.0")

    // Google fonts (Fraunces, Inter, Instrument Serif, JetBrains Mono)
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
