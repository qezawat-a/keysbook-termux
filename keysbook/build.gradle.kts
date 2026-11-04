plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing is supplied by CI (or a local keystore.properties) so no key
// material ever lives in the repository.
val keystoreFile = System.getenv("KEYSTORE_FILE")
val hasSigning = keystoreFile != null && file(keystoreFile).exists()

android {
    namespace = "com.keysbook.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.keysbook.app"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
    }

    if (hasSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigning) {
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

    // CodeAssist keeps Kotlin sources under src/main/kotlin.
    sourceSets["main"].java.srcDirs("src/main/kotlin", "src/main/java")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
}