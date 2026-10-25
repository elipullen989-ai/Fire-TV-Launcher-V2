plugins {
    id("com.android.application")
}

android {
    namespace = "com.eli.cleanlauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.eli.cleanlauncher"
        minSdk = 22      // Fire OS 5 (Android 5.1) and newer
        targetSdk = 30   // Fire OS 8 is Android 11 (API 30)
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the APK installs over ADB without extra setup.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Don't let cosmetic lint warnings fail the release build.
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Intentionally no dependencies: nothing that can phone home or serve ads.
