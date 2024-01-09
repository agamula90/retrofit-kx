pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // mavenLocal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "RetrofitKx"
include(
    ":retrofit-kx-android-sample",
   // ":retrofit-kx-kotlin-sample",
    ":retrofit-kx-ksp",
    ":retrofit-kx"
)
