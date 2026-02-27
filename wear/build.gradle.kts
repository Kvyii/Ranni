import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Load signing credentials from local.properties (gitignored)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace  = "com.ranni.wear"
    compileSdk = 35

    defaultConfig {
        // Must match :app applicationId so the Wearable Data Layer routes messages correctly
        applicationId = "com.ranni.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0"
    }

    // Sign all build types (including debug) with the release keystore so the
    // Wearable Data Layer certificate check passes against the phone APK.
    signingConfigs {
        create("release") {
            storeFile     = rootProject.file(localProps.getProperty("KEYSTORE_PATH", ""))
            storePassword = localProps.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias      = localProps.getProperty("KEY_ALIAS", "")
            keyPassword   = localProps.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            // Use release signing so watch ↔ phone Data Layer cert check passes
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            signingConfig   = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Wear Compose UI components
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)

    // Standard Compose runtime — BOM aligns versions with :app
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Wearable Data Layer API — DataClient reads + MessageClient sends
    implementation(libs.play.services.wearable)
    // .await() extension for Google Tasks (required for coroutine-based Data Layer calls)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.ui.tooling)
}
