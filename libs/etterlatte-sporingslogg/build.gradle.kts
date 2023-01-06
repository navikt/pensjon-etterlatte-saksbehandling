import Logging.Slf4jApi

plugins {
    kotlin("jvm")
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(project(":libs:common"))
    runtimeOnly(group = "com.papertrailapp", name = "logback-syslog4j", version = "1.0.0")

    compileOnly(Slf4jApi)

    testImplementation(Jupiter.Api)
    testRuntimeOnly(Jupiter.Engine)
    testImplementation(Kotest.AssertionsCore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}