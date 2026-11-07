plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Load local keystore properties if present, for debug/release signing during local dev.
val localKeystorePropsFile = rootProject.file("keystore.properties")
val localKeystoreProps = java.util.Properties()
var localHasUsableKeystore = false
if (localKeystorePropsFile.canRead()) {
    localKeystorePropsFile.inputStream().use { localKeystoreProps.load(it) }
    val storeFile = rootProject.file(localKeystoreProps.getProperty("storeFile", ""))
    localHasUsableKeystore = storeFile.exists()
}

val ciKeystoreFile = System.getenv("KEYSTORE_FILE")
val ciHasSigning = ciKeystoreFile != null && file(ciKeystoreFile).exists()

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
        } else if (localHasUsableKeystore) {
            create("release") {
                storeFile = rootProject.file(localKeystoreProps.getProperty("storeFile", "debug.keystore"))
                storePassword = localKeystoreProps.getProperty("storePassword", "android")
                keyAlias = localKeystoreProps.getProperty("keyAlias", "androiddebugkey")
                keyPassword = localKeystoreProps.getProperty("keyPassword", "android")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (ciHasSigning || localHasUsableKeystore) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Fallback to debug signing (automatically generated) so APK is installable.
                signingConfig = signingConfigs.getByName("debug")
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

    sourceSets["main"].java.srcDirs("src/main/kotlin", "src/main/java")
}

dependencies {
    implementation("androidx.databinding:viewbinding:8.7.3")
}