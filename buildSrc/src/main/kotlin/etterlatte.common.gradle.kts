
import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(kotlin("stdlib"))

    // Metrics
    implementation(libs.metrics.micrometer.prometheus)

    // Logging
    implementation(libs.logging.slf4japi)
    implementation(libs.logging.logbackclassic)
    implementation(libs.logging.logstashlogbackencoder) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    // Testing
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
}

tasks {
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.etterlatte.ApplicationKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }

        val configuration =
            configurations.runtimeClasspath.get().map {
                it.toPath().toFile()
            }
        val buildDirectory = layout.buildDirectory
        doLast {
            configuration.forEach {
                val file =
                    buildDirectory
                        .file("libs/${it.name}")
                        .get()
                        .asFile
                if (!file.exists()) {
                    it.copyTo(file)
                }
            }
        }
    }
}

tasks.test {
    environment(mapOf("TEST_RUNNER" to "true"))
}
