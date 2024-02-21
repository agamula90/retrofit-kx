import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    id("retrofitx.publish")
}

val properties: Properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

val sourcesJar = tasks.create<Jar>("librarySources") {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

val javadocJar = tasks.create<Jar>("libraryDocs") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.agamula90"
            artifactId = "retrofit-kx"
            version = publishEnvironment.releaseVersion

            artifact(javadocJar)
            artifact(sourcesJar)
            from (components["java"])
            // Provide artifacts information required by Maven Central
            pom {
                name.set("Retrofit-kx")
                description.set("Wrapper around [Retrofit:https://github.com/square/retrofit] library, that provides kotlin friendly api.")
                url.set("https://github.com/agamula90/RetrofitKx")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("agamula90")
                        name.set("Andrii Hamula")
                        email.set("andriihamula@gmail.com")
                        url.set("https://github.com/agamula90")
                    }
                }
                scm {
                    url.set("https://github.com/agamula90/RetrofitKx")
                    connection.set("scm:git:git://github.com/agamula90/RetrofitKx.git")
                    developerConnection.set("scm:git:ssh://git@github.com/agamula90/RetrofitKx.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            setUrl(publishEnvironment.deploymentUrl)
            credentials {
                username = properties["username"].toString()
                password = properties["password"].toString()
            }
        }
    }
}

signing {
    val publishKeyId = properties["publishKeyId"].toString()
    val publishSecret = properties["publishSecret"].toString()
    val publishPassword = properties["publishPassword"].toString()
    useInMemoryPgpKeys(publishKeyId, publishSecret, publishPassword)
    sign(publishing.publications.getByName("maven"))
}

dependencies {
    api("com.squareup.moshi:moshi-kotlin:1.14.0")
    api("com.squareup.retrofit2:retrofit:2.9.0")
    api("com.squareup.retrofit2:converter-moshi:2.9.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    kspTest("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.9")
}