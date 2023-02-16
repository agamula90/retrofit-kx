package retrofitx.publish

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.kotlin.dsl.add
import javax.inject.Inject

private val publishEnvironment = Environment.create(
    isReleaseReady = true,
    baseVersion = "0.0.1"
)

class PublishPlugin @Inject constructor(

) : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.add(
            publicType = Environment::class,
            name = "publishEnvironment",
            extension = publishEnvironment
        )
    }
}