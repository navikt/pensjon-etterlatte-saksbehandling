import org.gradle.api.tasks.testing.logging.TestLogEvent

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
    implementation(Logging.Slf4jApi)
    implementation(Logging.LogbackClassic)
    implementation(Logging.LogstashLogbackEncoder)

    // JUnit Testing
    testImplementation(Jupiter.Api)
    testImplementation(Jupiter.Params)
    testRuntimeOnly(Jupiter.Engine)
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
                if (!file.exists())
                    it.copyTo(file)
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
