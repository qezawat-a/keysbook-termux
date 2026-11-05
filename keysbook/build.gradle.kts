plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ciKeystoreFile = System.getenv("KEYSTORE_FILE")
val ciHasSigning = ciKeystoreFile != null && file(ciKeystoreFile).exists()
val localKeystoreProps = rootProject.file("keystore.properties")
val localHasSigning = localKeystoreProps.canRead()

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

    signingConfigs {
        if (ciHasSigning) {
            create("release") {
                storeFile = file(ciKeystoreFile!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        } else if (localHasSigning) {
            create("release") {
                storeFile = rootProject.file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (ciHasSigning || localHasSigning) {
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
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.databinding:viewbinding:8.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("com.google.firebase:firebase-analytics:")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
}