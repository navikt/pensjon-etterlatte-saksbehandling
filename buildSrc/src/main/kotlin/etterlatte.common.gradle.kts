import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestLogEvent

val libs = the<LibrariesForLibs>()

plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(kotlin("stdlib"))

    // Logging
    implementation(libs.logging.slf4japi)
    implementation(libs.logging.logbackclassic)
    implementation(libs.logging.logstashlogbackencoder)

    // JUnit Testing
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
}

tasks {
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.etterlatte.ApplicationKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists()) {
                    it.copyTo(file)
                }
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}