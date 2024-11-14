plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:etterlatte-ktor")) {
        exclude("io.ktor:ktor-server-cio", "io.ktor:ktor-server-metrics-micrometer")
    }
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-utbetaling-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-brev-model"))
    implementation(project(":libs:etterlatte-migrering-model"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:rapidsandrivers-extras"))

    testImplementation(libs.ktor2.clientcontentnegotiation)
    testImplementation(libs.ktor2.jackson)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:etterlatte-funksjonsbrytere"))))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
