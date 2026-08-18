import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}












 
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

// Gated HF repo access for the on-device ASR model download (AsrModelRepositoryImpl) - never
// checked into version control, same local.properties pattern as aiServiceAuthority above.
val hfAccessToken: String = run {
    val localProperties = rootProject.file("local.properties")
    val fromLocal: String? = if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties.getProperty("almahir.hfToken")
    } else {
        null
    }
    fromLocal ?: (findProperty("almahir.hfToken") as String?) ?: ""
}

val aiServiceToken: String = run {
    val localProperties = rootProject.file("local.properties")
    val fromLocal: String? = if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties.getProperty("almahir.aiToken")
    } else {
        null
    }
    fromLocal ?: (findProperty("almahir.aiToken") as String?) ?: ""
}

// Local dev servers (emulator loopback, LAN IP, or a plain "localhost" tajwid-serve) never have
// a TLS cert - only a real hostname (ngrok, production) does. Without "localhost"/"127.0.0.1"
// here, almahir.aiService=localhost:8100 was wrongly treated as secure, so the client tried WSS
// against a plaintext server and every live-correction session failed until reconnects exhausted.
val aiServiceIsLocal: Boolean =
    aiServiceAuthority.contains("192.168") ||
        aiServiceAuthority.startsWith("10.") ||
        aiServiceAuthority.contains("localhost") ||
        aiServiceAuthority.contains("127.0.0.1")

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
        buildConfigField("String", "HF_ACCESS_TOKEN", "\"$hfAccessToken\"")
        buildConfigField("String", "AI_SERVICE_TOKEN", "\"$aiServiceToken\"")
        buildConfigField("Boolean", "AI_SERVICE_SECURE", "${!aiServiceIsLocal}")
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

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    
    implementation(libs.androidx.datastore.preferences)

    
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // On-device streaming ASR for the local cursor-tracking path (see recite/local/asr).
    implementation(libs.sherpa.onnx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqlite.jdbc)
    
    testImplementation(platform(libs.ktor.bom))
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.server.websockets)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
