plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.toparla.spike"
    compileSdk = libs.versions.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.toparla.spike"
        minSdk = libs.versions.sdk.get().toInt()
        targetSdk = libs.versions.sdk.get().toInt()
        versionCode = 1
        versionName = "s0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.litertlm)
}
