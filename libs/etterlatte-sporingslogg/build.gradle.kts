plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    runtimeOnly(group = "com.papertrailapp", name = "logback-syslog4j", version = "1.0.0")

    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
