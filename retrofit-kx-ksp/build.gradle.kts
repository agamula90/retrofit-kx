import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

plugins {
    id ("org.jetbrains.kotlin.jvm")
    id ("com.google.devtools.ksp")
    id ("com.github.johnrengelman.shadow")
    id ("maven-publish")
}

group = "com.github.agamula90"

publishing {
    publications.withType<MavenPublication> {
        artifact("libs/retrofit-kx-ksp.jar")
        artifactId = "retrofitx"

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

val shade: Configuration = configurations.maybeCreate("compileShaded")
configurations.getByName("compileOnly").extendsFrom(shade)

val relocateShadowJar = tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
    target = tasks.shadowJar.get()
}

val shadowJar = tasks.shadowJar.apply {
    configure {
        dependsOn(relocateShadowJar)
        archiveClassifier.set("")
        archiveBaseName.set("shadow")
        configurations = listOf(shade)
    }
}

artifacts {
    runtimeOnly(shadowJar)
    archives(shadowJar)
}

dependencies {
    //TODO optimise
    shade ("com.squareup:kotlinpoet:1.12.0") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "com.google.guava")
    }
    implementation ("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
    implementation(project(":retrofit-kx"))
    compileOnly ("com.google.devtools.ksp:symbol-processing-api:1.7.20-1.0.7")

    testImplementation ("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation ("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.9")

    testImplementation ("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
}