setupTestLogging()

fun hentAntallKjerner(): Int {
    val isGithubWorkflow = System.getenv("CI")
    if (isGithubWorkflow != null) {
        return Runtime.getRuntime().availableProcessors()
    }
    return Runtime.getRuntime().availableProcessors() / 2
}

fun Project.setupTestLogging() {
    for (sub in subprojects) {
        sub.tasks.withType<Test> {
            maxParallelForks = hentAntallKjerner()
            testLogging {
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }
}
