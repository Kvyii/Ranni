import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Load signing credentials from local.properties (gitignored)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.ranni.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ranni.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "3.0.0"
    }

    // Sign release builds with the release keystore from local.properties
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
            buildConfigField("Boolean", "SHOW_DEV_TOOLS", "true")
        }
        release {
            isMinifyEnabled = false
            signingConfig   = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "SHOW_DEV_TOOLS", "false")
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
        buildConfig = true
    }
}

// Export Room schemas for AutoMigration support
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Animated vector support via View system (AnimatedVectorDrawableCompat)
    implementation("androidx.vectordrawable:vectordrawable-animated:1.2.0")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.calendar.compose)
    implementation(libs.androidx.splashscreen)
    ksp(libs.androidx.room.compiler)
    // Wearable Data Layer — writes favourite gym and receives climb messages from watch
    implementation(libs.play.services.wearable)
    // .await() extension for Google Tasks (required for coroutine-based Data Layer calls)
    implementation(libs.kotlinx.coroutines.play.services)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
