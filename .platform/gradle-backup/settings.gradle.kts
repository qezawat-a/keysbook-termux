// CodeAssist builds this project natively from module.toml.
// These Gradle files exist so GitHub Actions (and a desktop JVM) can also build it.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "keysbook-termux"
include(":keysbook")