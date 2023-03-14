plugins {
    kotlin("jvm")
}

repositories {
    maven("https://github.com")
    mavenCentral()
}

dependencies {
    implementation(project(":libs:common"))

    testImplementation(libs.test.jupiter.engine)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}