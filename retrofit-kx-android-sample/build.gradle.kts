plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("org.jetbrains.kotlin.plugin.serialization")
    id ("com.google.dagger.hilt.android")
    id ("com.google.devtools.ksp")
    id ("kotlin-kapt")
    id ("kotlin-parcelize")
    id ("retrofitx.publish")
}

android {
    namespace = "io.github.retrofitx.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.retrofitx.android"
        minSdk = 21
        targetSdk = 34
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures.buildConfig = true
}

dependencies {
    implementation ("androidx.core:core-ktx:1.12.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.11.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation ("androidx.datastore:datastore-preferences:1.0.0")

    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation ("com.google.dagger:hilt-android:2.48")
    ksp ("com.google.dagger:hilt-compiler:2.48")

    val sandwichVersion = "2.0.5"
    implementation ("com.github.skydoves:sandwich:$sandwichVersion")
    implementation("com.github.skydoves:sandwich-retrofit-serialization:$sandwichVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    ksp ("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
    implementation ("com.squareup.moshi:moshi-kotlin:1.14.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-moshi:2.9.0")

    debugImplementation ("com.github.YarikSOffice.Venom:venom:0.7.1")
    releaseImplementation ("com.github.YarikSOffice.Venom:venom-no-op:0.7.1")
}
