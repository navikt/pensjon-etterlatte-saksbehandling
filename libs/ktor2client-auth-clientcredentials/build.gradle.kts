plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    // maven("https://kotlin.bintray.com/ktor")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.ktor2.okhttp)
    api(libs.ktor2.clientcore)
    api(libs.ktor2.clientauth)
    api(libs.ktor2.clientcontentnegotiation)
    api(libs.ktor2.jackson)
    api(libs.ktor2.clientloggingjvm)
    api(project(":libs:etterlatte-token-model"))

    api(libs.navfelles.tokenclientcore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}