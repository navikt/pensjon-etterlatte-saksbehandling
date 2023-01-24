import java.io.FileOutputStream
import java.util.*

plugins {
    kotlin("jvm")
}

version = "0.1"

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    api(Jackson.DatatypeJsr310)
    api(Jackson.ModuleKotlin)
    testImplementation(Jupiter.Api)
    testRuntimeOnly(Jupiter.Engine)
    testImplementation(Kotest.AssertionsCore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}

val generatedVersionDir = "$buildDir/generated-version"

sourceSets {
    main {
        kotlin {
            output.dir(generatedVersionDir)
        }
    }
}

tasks.register("generateVersionProperties") {
    doLast {
        val propertiesFile = file("$generatedVersionDir/version.properties")
        propertiesFile.parentFile.mkdirs()
        val properties = Properties()
        properties.setProperty("version", "$version")
        val out = FileOutputStream(propertiesFile)
        properties.store(out, null)
    }
}

tasks.named("processResources") {
    dependsOn("generateVersionProperties")
}