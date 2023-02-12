plugins {
    id ("org.jetbrains.kotlin.jvm")
    id ("com.google.devtools.ksp")
}

sourceSets.getByName("main") {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
}

ksp {
    arg("servicesPackage", "com.github.retrofitx.kotlin.remote")
}

dependencies {
    ksp (project(":retrofit-kx-ksp"))
    implementation (project(":retrofit-kx"))
}