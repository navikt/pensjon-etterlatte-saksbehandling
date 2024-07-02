plugins {
    alias(libs.plugins.avro)
    id("etterlatte.common")
}

repositories {
    maven("https://maven.pkg.github.com/navikt/teamdokumenthandtering-avro-schemas")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-oppgave-model"))

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
    implementation(libs.kafka.avro) {
        exclude("org.apache.commons", "commons-compress")
    }
    implementation(libs.commons.compress)

    implementation(libs.kafka.avroserializer)

    implementation(libs.teamdokumenthandtering.avroschemas)

    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.ktor2.clientmock)
}
