plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

repositories {
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.bundles.jackson)
    implementation(project(":libs:saksbehandling-common"))
    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.avroserializer)
    testImplementation(libs.kafka.embeddedenv)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}