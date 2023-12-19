setupTestLogging()

fun Project.setupTestLogging() {
    for (sub in subprojects) {
        sub.tasks.withType<Test> {
            testLogging {
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        }
    }
}
