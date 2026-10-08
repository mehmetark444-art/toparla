plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
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
    implementation(libs.room.runtime)
    implementation(libs.sqlite.bundled)
    ksp(libs.room.compiler)
}
