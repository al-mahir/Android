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
