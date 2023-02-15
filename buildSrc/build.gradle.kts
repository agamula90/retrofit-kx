plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins.register("publishPlugin") {
        id = "retrofitx.publish"
        implementationClass = "retrofitx.publish.PublishPlugin"
    }
}