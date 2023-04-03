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
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.bundles.jackson)
    implementation(project(":libs:common"))
    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
    implementation(libs.kafka.clients)
    testImplementation(libs.kafka.embeddedenv)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}