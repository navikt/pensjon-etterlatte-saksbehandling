plugins {
    kotlin("jvm")
    application
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
