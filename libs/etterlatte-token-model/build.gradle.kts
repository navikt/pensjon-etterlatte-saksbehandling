
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    testImplementation(Jupiter.Api)
    testImplementation(Jupiter.Engine)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}