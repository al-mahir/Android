plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.mushaf.domain"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Framework-free domain layer: Kotlin + Coroutines/Flow only. No Room/Compose/UI deps.
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":domain"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
