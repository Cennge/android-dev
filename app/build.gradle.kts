plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.android_proj"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.android_proj"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    androidResources {
        // keep the .glb model uncompressed so Filament can read it directly
        noCompress += "glb"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)

    // 1. AndroidX Media3 (ExoPlayer) — video / audio playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    // 2. Google Filament — physically based 3D rendering + glTF loader
    implementation(libs.filament.android)
    implementation(libs.filament.gltfio)
    implementation(libs.filament.utils)
    // 3. Airbnb Lottie — After Effects vector animations
    implementation(libs.lottie)
    // 4. Coil 3 — image loading (memory/disk cache, transformations)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    // 5. Konfetti — particle system (XML/View version)
    implementation(libs.konfetti.xml)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}