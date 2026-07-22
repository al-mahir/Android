import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Where the Al-Mahir AI service lives, for this machine.
 *
 * Set `almahir.aiService` in `local.properties` (git-ignored, so a LAN address never lands in
 * VCS) or pass `-Palmahir.aiService=…`. The default is the Android **emulator's** alias for the
 * host loopback — a physical device cannot reach it, which is the single most common reason a
 * live session silently fails to connect.
 *
 *   Emulator .............. 10.0.2.2:8100        (default)
 *   Device + adb reverse .. localhost:8100       after `adb reverse tcp:8100 tcp:8100`
 *   Device over Wi-Fi ..... 192.168.1.3:8100     your laptop's LAN IP, needs a firewall rule
 */
val aiServiceAuthority: String = run {
    val localProperties = rootProject.file("local.properties")
    val fromLocal: String? = if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties.getProperty("almahir.aiService")
    } else {
        null
    }
    fromLocal ?: (findProperty("almahir.aiService") as String?) ?: "10.0.2.2:8100"
}

android {
    namespace = "com.example.mushaf.data"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AI_SERVICE_AUTHORITY", "\"$aiServiceAuthority\"")
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Forward the live-smoke-test knobs into the test JVM. Gradle's own -D flags stop at the Gradle
// daemon, so without this LiveServerSmokeTest silently falls back to its defaults and looks as
// though it ignored the arguments.
tasks.withType<Test>().configureEach {
    listOf("almahirServer", "almahirWav").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
}

dependencies {
    implementation(project(":mushaf:domain"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Room (read-only, mounted from assets)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Preferences persistence
    implementation(libs.androidx.datastore.preferences)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    // Serialization & Networking
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqlite.jdbc)
    // An in-process stand-in for the AI service; see LiveRecitationSocketTest.
    testImplementation(platform(libs.ktor.bom))
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.server.websockets)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
