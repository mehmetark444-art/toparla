plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.toparla.app"
    compileSdk = libs.versions.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.toparla.app"
        minSdk = libs.versions.sdk.get().toInt()
        targetSdk = libs.versions.sdk.get().toInt()
        // versionCode = yyMMddNN (blueprint B7)
        versionCode = 26100701
        versionName = "0.0.1"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":reminders"))
    implementation(project(":ai"))
    implementation(project(":sensors"))
    implementation(project(":ui"))
}
