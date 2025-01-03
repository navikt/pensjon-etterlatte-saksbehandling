plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(project(":libs:etterlatte-inntektsjustering-model"))
    implementation(libs.etterlatte.common)

    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:rapidsandrivers-extras"))

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.ktor2.clientcontentnegotiation)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
