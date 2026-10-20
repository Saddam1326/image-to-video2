plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wood.exk.imagetovideo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wood.exk.imagetovideo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }
    implementation("com.github.arthenica:ffmpeg-kit-full:6.0-2.LTS")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2.LTS")
    implementation("org.opencv:opencv:4.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
