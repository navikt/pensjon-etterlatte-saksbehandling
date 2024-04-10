setupRepositories()
setupTestLogging()

fun hentAntallKjerner(): Int {
    val isGithubWorkflow = System.getenv("CI")
    if (isGithubWorkflow != null) {
        return Runtime.getRuntime().availableProcessors()
    }
    return Runtime.getRuntime().availableProcessors() / 2
}
plugins {
    alias(libs.plugins.license) apply true
    alias(libs.plugins.versions) apply true
}

fun Project.setupRepositories() {
    for (sub in subprojects) {
        sub.repositories {
            mavenCentral()
            maven {
                url = uri("https://maven.pkg.github.com/navikt/pensjon-etterlatte-libs")
                credentials {
                    username = "token"
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
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
