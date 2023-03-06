
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}