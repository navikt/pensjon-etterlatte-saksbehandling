plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:saksbehandling-common"))
    implementation(libs.kafka.clients)
    implementation(libs.kafka.avroserializer)
    implementation(libs.ktor2.okhttp)
}