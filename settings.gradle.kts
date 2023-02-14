pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "RetrofitKx"
include(
    ":retrofit-kx-android-sample",
    ":retrofit-kx-kotlin-sample",
    ":retrofit-kx-ksp",
    ":retrofit-kx"
)
