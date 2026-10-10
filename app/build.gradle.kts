import java.time.Duration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

// İmza bilgisi depoya girmez (blueprint B7). Dosya yoksa release imzasız derlenir ve telefona kurulamaz.
val keystoreFile = rootProject.file("keystore.properties")
val keystore = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) } }

// Bir test görevinin en uzun süresi (dakika).
val testTimeoutMinutes = 10L

android {
    namespace = "com.toparla.app"
    compileSdk = libs.versions.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.toparla.app"
        minSdk = libs.versions.sdk.get().toInt()
        targetSdk = libs.versions.sdk.get().toInt()
        // versionCode = yyMMddNN (blueprint B7): yıl, ay, gün, o günkü sıra. Her kurulan sürümde artar.
        versionCode = 26101002
        versionName = "0.0.5"
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Gerçek veriye dokunmasın diye ayrı paket; release ile yan yana kurulur.
            applicationIdSuffix = ".dev"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Ekranların ekran görüntüsü ve Compose testleri (Roborazzi + Robolectric) JVM'de koşar; düzen `:ui` ile aynı.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Robolectric'in JDK 17+ gereksinimi (robolectric.github.io "getting-started"; proje beyni H31).
                it.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                )
                it.timeout.set(Duration.ofMinutes(testTimeoutMinutes))
                it.testLogging {
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    events("failed")
                }
            }
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":reminders"))
    implementation(project(":ai"))
    implementation(project(":sensors"))
    implementation(project(":ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.serialization.json)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.timber)
    debugImplementation(libs.leakcanary)
    // Yalnız debug tetikleyicisi için (güvenlik ağı işlerini beklemeden koşturma).
    debugImplementation(libs.work.runtime)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
}
