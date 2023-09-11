plugins {
    alias(libs.plugins.avro)
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))

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

    testImplementation(libs.kafka.embeddedenv)
    testImplementation(libs.el.api)
    testImplementation(libs.el.impl)
    testImplementation(libs.ktor2.servertests)
}