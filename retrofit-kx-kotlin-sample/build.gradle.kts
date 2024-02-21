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
    implementation (project(":retrofit-kx"))
    ksp (project(":retrofit-kx-ksp"))
}