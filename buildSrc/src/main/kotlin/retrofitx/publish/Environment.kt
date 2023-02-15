package retrofitx.publish

private class EnvironmentImpl(
    private val isReleaseReady: Boolean,
    private val baseVersion: String
) : Environment {

    override val releaseVersion: String
        get() = run {
            if (isReleaseReady) baseVersion else "$baseVersion-SNAPSHOT"
        }

    override val deploymentUrl: String
        get() = when {
            isReleaseReady -> "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            else -> "https://s01.oss.sonatype.org/content/repositories/snapshots/"
        }
}

interface Environment {
    val releaseVersion: String
    val deploymentUrl: String

    companion object {
        fun create(
            isReleaseReady: Boolean,
            baseVersion: String
        ): Environment {
            return EnvironmentImpl(isReleaseReady, baseVersion)
        }
    }
}