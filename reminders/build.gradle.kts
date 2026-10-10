import java.time.Duration

// Bir test görevinin en uzun süresi (dakika).
val testTimeoutMinutes = 10L

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.toparla.reminders"
    compileSdk = libs.versions.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Android'e bağlı zorunlu senaryolar (F2.46) Robolectric ile JVM'de koşar; kaynakları (metinler) okur.
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
                )
                it.timeout.set(Duration.ofMinutes(testTimeoutMinutes))
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
    implementation(project(":data"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coroutines.android)
    implementation(libs.work.runtime)
    implementation(libs.timber)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.datastore.preferences)
}
