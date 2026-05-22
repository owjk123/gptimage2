plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.gptimage2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gptimage2.pro"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Optional default key injected from GitHub Secrets or local.properties
        val apiKey = project.findProperty("APIYI_KEY") as String? ?: ""
        buildConfigField("String", "APIYI_KEY", "\"$apiKey\"")
    }

    signingConfigs {
        create("appKey") {
            storeFile = rootProject.file("signing/gptimage2.keystore")
            storePassword = "gptimage2"
            keyAlias = "gptimage2"
            keyPassword = "gptimage2"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("appKey")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("appKey")
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
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.androidx.window.size.cls)
    debugImplementation(libs.androidx.ui.tooling)
}
