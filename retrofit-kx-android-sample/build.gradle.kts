plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("com.google.devtools.ksp")
    id ("com.google.dagger.hilt.android")
    id ("kotlin-kapt")
    id ("kotlin-parcelize")
    id ("retrofitx.publish")
}

android {
    namespace = "io.github.retrofitx.android"
    compileSdk = 33

    defaultConfig {
        applicationId = "io.github.retrofitx.android"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("debug") {
            buildConfigField ("String", "BASE_URL", "\"https://vast-berry-paranthodon.glitch.me/\"")
        }

        getByName("release") {
            buildConfigField ("String", "BASE_URL", "\"https://vast-berry-paranthodon.glitch.me/\"")
            isMinifyEnabled = false
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets.getByName("main") {
        buildTypes.names.forEach { buildType ->
            kotlin.srcDir("build/generated/ksp/$buildType/kotlin")
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

ksp {
    arg("servicesPackage", "io.github.retrofitx.android.remote")
}

dependencies {
    implementation ("androidx.core:core-ktx:1.9.0")
    implementation ("androidx.appcompat:appcompat:1.5.1")
    implementation ("com.google.android.material:material:1.7.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation ("androidx.datastore:datastore-preferences:1.0.0")

    implementation ("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation ("com.google.dagger:hilt-android:2.44")
    kapt ("com.google.dagger:hilt-compiler:2.44")

    implementation ("io.github.agamula90:retrofit-kx:${publishEnvironment.releaseVersion}")
    ksp ("io.github.agamula90:retrofit-kx-ksp:${publishEnvironment.releaseVersion}")

    debugImplementation ("com.github.YarikSOffice.Venom:venom:0.5.0")
    releaseImplementation ("com.github.YarikSOffice.Venom:venom-no-op:0.5.0")
}
