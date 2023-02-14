plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.agamula90"
            artifactId = "retrofit-kx"
            version = "0.0.1"

            from (components.getByName("java"))
            // Provide artifacts information required by Maven Central
            pom {
                name.set("Retrofit-kx")
                description.set("test retrofit-kx")
                url.set("https://github.com/agamula90/RetrofitKx")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("https://github.com/agamula90")
                        name.set("Andrii Hamula")
                        email.set("andriihamula@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/agamula90/RetrofitKx")
                }
            }
        }
    }
}

dependencies {
    api ("com.squareup.moshi:moshi-kotlin:1.14.0")
    api ("com.squareup.retrofit2:retrofit:2.9.0")
    api ("com.squareup.retrofit2:converter-moshi:2.9.0")
    api ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    kspTest("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.9")
}