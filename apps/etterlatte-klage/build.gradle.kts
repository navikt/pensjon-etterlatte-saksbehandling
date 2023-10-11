plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

val klageKodeverkVersion = "1.6.10"

dependencies {
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:saksbehandling-common"))
    implementation("no.nav.klage:klage-kodeverk:$klageKodeverkVersion")

    implementation(libs.kafka.clients)
    implementation(libs.kafka.avroserializer)
    implementation(libs.ktor2.okhttp)
}
