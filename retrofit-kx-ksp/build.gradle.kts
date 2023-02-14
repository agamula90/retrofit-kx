plugins {
    id ("org.jetbrains.kotlin.jvm")
    id ("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.agamula90"
            artifactId = "retrofit-kx-ksp"
            version = "0.0.2"

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
    compileOnly ("com.google.devtools.ksp:symbol-processing-api:1.7.20-1.0.7")
    implementation ("com.squareup:kotlinpoet:1.12.0")
    implementation ("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")

    implementation (project(path = ":retrofit-kx"))

    testImplementation ("com.google.devtools.ksp:symbol-processing-api:1.7.20-1.0.7")
    testImplementation ("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation ("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.9")
}