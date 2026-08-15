pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // sherpa-onnx (on-device streaming ASR) is only published via JitPack, not Maven Central.
        maven("https://jitpack.io")
        // Paymob Android SDK is distributed as a local AAR — not published to Maven Central.
        // Place the AAR at: libs/com/paymob/sdk/Paymob-SDK/<version>/Paymob-SDK-<version>.aar
        maven { url = rootProject.projectDir.toURI().resolve("libs") }
    }
}

rootProject.name = "Al-Mahir"
include(":app")
include(":sheikh-app")
include(":sheikh:presentation")
include(":mushaf:domain")
include(":mushaf:data")
include(":mushaf:presentation")
include(":designsystem")
include(":domain")
include(":data")
include(":presentation")
include(":meeting:domain")
include(":meeting:data")
include(":meeting:presentation")
