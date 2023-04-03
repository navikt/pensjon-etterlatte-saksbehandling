plugins {
    kotlin("jvm")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/navikt/pensjon-etterlatte-libs")
        credentials {
            username = "token"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    maven("https://jitpack.io")
    mavenCentral()
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