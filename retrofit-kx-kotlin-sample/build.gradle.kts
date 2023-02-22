plugins {
    id ("org.jetbrains.kotlin.jvm")
    id ("com.google.devtools.ksp")
    id ("retrofitx.publish")
}

sourceSets.getByName("main") {
    kotlin.srcDir("build/generated/ksp/$name/kotlin")
}

ksp {
    arg("servicesPackage", "io.github.retrofitx.kotlin.remote")
}

dependencies {
    implementation ("io.github.agamula90:retrofit-kx:${publishEnvironment.releaseVersion}")
    ksp ("io.github.agamula90:retrofit-kx-ksp:${publishEnvironment.releaseVersion}")
}