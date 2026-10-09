import java.time.Duration

// Bir test görevinin en uzun süresi (dakika).
val testTimeoutMinutes = 10L

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.toparla.ui"
    compileSdk = libs.versions.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Ekran görüntüsü testleri (Roborazzi + Robolectric) JVM'de koşar ve kaynakları okur.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Robolectric, JDK 17+ üzerinde JDK iç sınıflarına erişmek için bu açılımları ister
                // (robolectric.github.io "getting-started"; ilk CI koşusunda IllegalAccessException).
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
                // Takılan test (ör. hiç durulmayan animasyon) koşuyu 30 dk bekletmesin.
                it.timeout.set(Duration.ofMinutes(testTimeoutMinutes))
                // CI günlüğünde hata yığını tam görünsün (rapor dosyası her ortamdan indirilemiyor).
                it.testLogging {
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    events("failed")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":domain"))

    // Tema `ColorScheme` ve `Typography` türlerini dışa verdiği için Material 3 ve Compose UI dışa açık.
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
