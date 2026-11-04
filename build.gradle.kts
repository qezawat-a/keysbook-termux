// Root build file. The app module is :keysbook (see settings.gradle.kts).
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
}

// Do not check in the gradle wrapper; CodeAssist copies it locally.
wrapper {
    gradleVersion = "8.13"
}