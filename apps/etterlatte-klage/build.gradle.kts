plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:rapidsandrivers-extras"))

    implementation(libs.klage.kodeverk)

    testImplementation(libs.test.jupiter.api)
}
