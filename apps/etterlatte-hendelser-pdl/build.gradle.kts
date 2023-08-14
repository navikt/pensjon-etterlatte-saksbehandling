plugins {
    alias(libs.plugins.avro)
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-pdl-model"))

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientloggingjvm)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.servercio)
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
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.ktor2.clientmock)
}

tasks.named("compileKotlin").configure { dependsOn(":apps:etterlatte-hendelser-pdl:generateAvroJava") }
tasks.named("compileTestKotlin").configure { dependsOn(":apps:etterlatte-hendelser-pdl:generateAvroJava") }