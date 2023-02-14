plugins {
    id ("org.jetbrains.kotlin.jvm")
    id ("com.google.devtools.ksp")
}

sourceSets.getByName("main") {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
}

ksp {
    arg("servicesPackage", "io.github.retrofitx.kotlin.remote")
}

dependencies {
    ksp ("io.github.agamula90:retrofit-kx-ksp:0.0.2")
    implementation ("io.github.agamula90:retrofit-kx:0.0.1")
}