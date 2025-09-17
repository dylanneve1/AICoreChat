plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.dylanneve1.aicorechat"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.dylanneve1.aicorechat"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkAllWarnings = true
        explainIssues = true
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.ai.edge.aicore:aicore:0.0.1-exp01")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Markdown renderer for Compose Material 3
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.26.0")

    // ML Kit GenAI Image Description
    implementation("com.google.mlkit:genai-image-description:1.0.0-beta1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")

    // Image loading for Compose
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation("org.robolectric:robolectric:4.12.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
