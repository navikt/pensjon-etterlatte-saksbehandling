setupTestLogging()

fun Project.setupTestLogging() {
    for (sub in subprojects) {
        sub.tasks.withType<Test> {
            maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
            testLogging {
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }
}
