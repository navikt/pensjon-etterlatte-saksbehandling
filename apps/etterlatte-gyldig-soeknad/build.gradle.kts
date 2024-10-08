plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(libs.etterlatte.common)

    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:rapidsandrivers-extras"))

    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.clientauth)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.ktor2.clientcontentnegotiation)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
