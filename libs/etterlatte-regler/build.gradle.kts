import java.io.FileOutputStream
import java.util.Properties

plugins {
    kotlin("jvm")
}

version = "1.1"

dependencies {
    api(libs.jackson.datatypejsr310)
    api(libs.jackson.modulekotlin)

    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}

val generatedVersionDir = layout.buildDirectory.dir("generated-version")

sourceSets {
    main {
        kotlin {
            output.dir(generatedVersionDir.get())
        }
    }
}

tasks.register("generateVersionProperties") {
    val get = generatedVersionDir.get()
    val propertiesFile = file("$get/version.properties")
    propertiesFile.parentFile.mkdirs()
    val properties = Properties()
    properties.setProperty("version", "$version")
    val out = FileOutputStream(propertiesFile)
    properties.store(out, null)
}

tasks.named("processResources") {
    dependsOn("generateVersionProperties")
}
