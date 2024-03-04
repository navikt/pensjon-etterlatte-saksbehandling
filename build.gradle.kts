setupTestLogging()

fun hentAntallKjerner(): Int {
    val isGithubWorkflow = System.getenv("CI")
    if (isGithubWorkflow != null) {
        return Runtime.getRuntime().availableProcessors()
    }
    return Runtime.getRuntime().availableProcessors() / 2
}
plugins {
    alias(libs.plugins.versions) apply true
}

fun Project.setupTestLogging() {
    for (sub in subprojects) {
        sub.plugins.apply(libs.plugins.versions.get().pluginId)
        sub.tasks.withType<Test> {
            maxParallelForks = hentAntallKjerner()
            testLogging {
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }
}
