@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.avro)
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.kafka")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:etterlatte-helsesjekk"))

    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.jackson)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.modulekotlin)
    implementation(libs.jackson.datatypejsr310)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.avro)
    implementation(libs.kafka.avroserializer)
    implementation(libs.logging.logbackclassic)

    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.kafka.embeddedenv)
    testImplementation(libs.test.jupiter.engine)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.mockk)
}

tasks {
    test {
        testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}