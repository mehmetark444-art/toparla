pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "toparla"

// Tek yönlü bağımlılık: blueprint B3.
include(":domain", ":data", ":reminders", ":ai", ":sensors", ":ui", ":app")

// S0 cihaz denemeleri; ürün koduna bağımlı değildir, S0 bitince kaldırılır.
include(":spike")
