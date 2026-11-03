plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Mirrors keysbook/module.toml so both build systems produce the same app.
android {
    namespace = "com.keysbook.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.keysbook.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0"
    }

    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
    sourceSets["main"].java.srcDirs("src/main/kotlin", "src/main/java")
    sourceSets["main"].res.srcDirs("src/main/res")

    signingConfigs {
        create("release") {
            // Supplied by the workflow from repository secrets.
            val storePath = System.getenv("KEYSBOOK_KEYSTORE_PATH")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = System.getenv("KEYSBOOK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEYSBOOK_KEY_ALIAS")
                keyPassword = System.getenv("KEYSBOOK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only sign when a keystore is actually configured (CI release runs);
            // otherwise produce an unsigned APK so local builds never fail.
            if (System.getenv("KEYSBOOK_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
}