plugins {
    id ("org.jetbrains.kotlin.jvm")
    id ("com.google.devtools.ksp")
}

dependencies {
    api ("com.squareup.moshi:moshi-kotlin:1.14.0")
    api ("com.squareup.retrofit2:retrofit:2.9.0")
    api ("com.squareup.retrofit2:converter-moshi:2.9.0")
    api ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    ksp ("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")

    testImplementation ("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation ("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.9")
}